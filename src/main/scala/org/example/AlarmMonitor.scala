package org.example

import com.alibaba.fastjson.JSON
import org.example.SendEmail.sendEmail

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object AlarmMonitor {
  def main(args: Array[String]): Unit = {
    //获取配置文件
    val properties = GetConfig.getProperties("test.properties")
    val mailTo = properties.getProperty("mail.to")
    val delayTime = properties.getProperty("delay.time").toInt

    val maxSendTime  =properties.getProperty("max.send.time").toInt
    //properties.getProperty("alarm.delayTime") 转为utf-8编码

    val alarmDelayTimeUTF8 = new String(properties.getProperty("alarm.delayTime").getBytes("ISO-8859-1"), "UTF-8")


    //取出每家车厂报警的延迟时间
    val alarmDelayTime = JSON.parseObject(alarmDelayTimeUTF8)

    //获取发送邮件的配置
    val start = properties.getProperty("start.hour").toInt
    val end = properties.getProperty("end.hour").toInt
    // 使用示例
    val from = "ids_alarm@gotion.com.cn"
    val password = properties.getProperty("password")
    val host = "smtp.exmail.qq.com"
    val port = "465"
    val subject = "延迟数据监控报警"
    //定义一个字符串，记录上次发的消息
    var lastMessage = ""
    //定义一个字符串，记录本次发的消息
    var currentMessage = ""
    //记录邮件发送的时间
    var lastTime: Long = 0
    while (true) {
      //查询mysql表
      val mySQLQuerySql = "select vehicle_factory_name,vehicle_factory_code from t_vehicle_factory WHERE vehicle_factory_code != '18'"
      val mySQLQueryTool = new MySQLQueryTool
      val set = mySQLQueryTool.executeQuery(mySQLQuerySql)
      //定义一个map
      val map = scala.collection.mutable.Map[String, String]()
      //遍历结果集
      while (set.next()) {
        val vehicleFactoryName = set.getString("vehicle_factory_name")
        val vehicleFactoryCode = set.getString("vehicle_factory_code")
        map += (vehicleFactoryCode -> vehicleFactoryName)
      }

      val currentDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
      val currentTime= LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
      val sql =
        s"""
           |SELECT vehicleFactory,max(ctime) as maxctime, 'ods' AS source_type,'$currentTime' AS currentTime
           |FROM source_gx.ods_all
           |WHERE day_of_year >= '$currentDate' AND ctime <= '$currentTime'
           |GROUP BY vehicleFactory,source_type,currentTime
           |UNION ALL
           |SELECT vehicleFactory,max(ctime) as maxctime, 'dwd' AS source_type,'$currentTime' AS currentTime
           |FROM warehouse_gx.dwd_all
           |WHERE day_of_year >= '$currentDate' AND ctime <= '$currentTime'
           |GROUP BY vehicleFactory,source_type,currentTime
           |UNION ALL
           |SELECT vehicle_factory as vehicleFactory,max(alarm_time) as maxctime, 'alarm' AS source_type,'$currentTime' AS currentTime
           |FROM battery_alarm.alarm_all
           |WHERE day_of_year >= '$currentDate' AND alarm_time <= '$currentTime'
           |GROUP BY vehicle_factory,source_type,currentTime
           |""".stripMargin

      val connection = new ClickhouseUtils().getConnection
      val resultSet = connection.createStatement().executeQuery(sql)
      //定义一个集合存放结果集
      val list = scala.collection.mutable.ListBuffer[String]()
      // 检查每条记录的时间差值
      while (resultSet.next()) {
        val vehicleFactory = resultSet.getString("vehicleFactory")
        val ctime = resultSet.getString("maxctime")
        val source_type = resultSet.getString("source_type")
        val currentTime = resultSet.getString("currentTime")
        //currentTime转为yyyy-MM-dd HH:mm:ss格式
        val currentTimeFormat = LocalDateTime.parse(currentTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        //ctime转为yyyy-MM-dd HH:mm:ss格式
        val ctimeFormat = LocalDateTime.parse(ctime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val minutesDiff = ChronoUnit.MINUTES.between(ctimeFormat, currentTimeFormat)
        if (source_type != "alarm" && minutesDiff > delayTime) {
          //获取当前车厂号
          val vehicleFactoryName = map.getOrElse(vehicleFactory, null)
          if (vehicleFactoryName != null) {
            val content = s"$vehicleFactoryName 的 $source_type 数据在 $ctime 时刻未更新，延迟 $minutesDiff 分钟！"
            currentMessage += s"$vehicleFactoryName 的 $source_type "
            list += content
          }
        } else if (source_type == "alarm") {
          val vehicleFactoryName = map.getOrElse(vehicleFactory, null)
          if (vehicleFactoryName != null) {
          //获取当前车厂号对应的延迟时间
          val alarmDelayTimeValue = alarmDelayTime.getString(vehicleFactoryName).toInt
          if(minutesDiff > alarmDelayTimeValue){
              val content = s"$vehicleFactoryName 的 $source_type 数据在 $ctime 时刻未更新，延迟 $minutesDiff 分钟！"
              currentMessage += s"$vehicleFactoryName 的 $source_type "
              list += content
            }
          }
        }
      }

      currentMessage
      //获取当前时间戳
      val nowTime: Long = System.currentTimeMillis()
      val diff: Long = nowTime - lastTime
//      println(lastMessage,currentMessage)
      if (list.nonEmpty && (!isAnagram(lastMessage,currentMessage) || (isAnagram(lastMessage,currentMessage) && diff>1000*60*60*maxSendTime))) {
        //每天的早上8点到晚上8点之间发送邮件
        val hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH")).toInt
        if (hour >= start && hour <= end) {
          sendEmail(from, password, mailTo.split(",").toList, host, port, subject, list.sorted.mkString("\n"))
        }
        //记录当前时间戳
        lastTime = nowTime
        //取出list中的第一条数据
      }
      lastMessage =currentMessage
      currentMessage = ""
      //睡眠
      Thread.sleep(delayTime*1000*60)
    }
  }
  def isAnagram(str1: String, str2: String): Boolean = {
    str1.toLowerCase.sorted == str2.toLowerCase.sorted
  }

}

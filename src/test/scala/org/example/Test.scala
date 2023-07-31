package org.example

import org.example.SendEmail.sendEmail

object Test {
  def main(args: Array[String]): Unit = {
    val from = "ids_alarm@gotion.com.cn"
    val password = "Battery@123"
    val host = "smtp.exmail.qq.com"
    val port = "465"
    val subject = "延迟数据监控报警"
    val mailTo="wangjie_zn@gotion.com.cn"
    sendEmail(from, password, mailTo.split(",").toList, host, port, subject, "111111")

  }

}

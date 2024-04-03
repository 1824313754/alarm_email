package org.example

import scala.beans.BeanProperty

/**
 * @Author WangJie
 * @Date 2024/4/3 11:01 
 * @description: ${description}
 * @Title: AlarmData
 * @Package org.example 
 */
class AlarmData {
  @BeanProperty var vehicleFactory:String = _
  @BeanProperty var ctime :String= _
  @BeanProperty var source_type :String= _
  @BeanProperty var currentTime :String= _
  //全参构造器
  def this(vehicleFactory:String, ctime:String, source_type:String, currentTime:String){
    this()
    this.vehicleFactory = vehicleFactory
    this.ctime = ctime
    this.source_type = source_type
    this.currentTime = currentTime
  }
}

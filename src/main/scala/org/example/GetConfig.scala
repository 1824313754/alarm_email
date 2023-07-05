package org.example

import java.io.FileInputStream
import java.util.Properties

object GetConfig {

  def getProperties(filename :String)={
    val properties = new Properties()
    properties.load(new FileInputStream(filename))
    properties
  }

}

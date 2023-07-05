package org.example

import org.example.GetConfig

import java.sql.{Connection, DriverManager, ResultSet}

class MySQLQueryTool {
  val properties = GetConfig.getProperties("test.properties")
  private val mysqlConnString = properties.getProperty("mysql.conn")
  private val mysqlUser = properties.getProperty("mysql.user")
  private val mysqlPassword = properties.getProperty("mysql.passwd")

  // 初始化连接
  private def getConnection: Connection = {
    DriverManager.getConnection(mysqlConnString, mysqlUser, mysqlPassword)
  }

  // 执行查询
  def executeQuery(sql: String): ResultSet = {
    val connection = getConnection
    val statement = connection.createStatement()
    statement.executeQuery(sql)
  }

  // 关闭连接
  def close(): Unit = {
    getConnection.close()
  }
}

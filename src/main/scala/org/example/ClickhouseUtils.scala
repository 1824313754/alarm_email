package org.example

import ru.yandex.clickhouse.{BalancedClickhouseDataSource, ClickHouseConnection}
import ru.yandex.clickhouse.settings.ClickHouseProperties

class ClickhouseUtils extends Serializable {
  @volatile private var connection: ClickHouseConnection = _
  val properties = GetConfig.getProperties("test.properties")
  val ckTable: String = properties.getProperty("clickhouse.table")
  def getConnection: ClickHouseConnection = {
    if (connection == null) {
      synchronized {
        if (connection == null) {
          try {
            val clickPro = new ClickHouseProperties()
            clickPro.setUser(properties.getProperty("clickhouse.user"))
            clickPro.setPassword(properties.getProperty("clickhouse.passwd"))
            val source: BalancedClickhouseDataSource = new BalancedClickhouseDataSource(properties.getProperty("clickhouse.conn"), clickPro)
            source.actualize()
            connection = source.getConnection
          } catch {
            case e: Exception =>
              e.printStackTrace()
          }
        }
      }
    }
    connection
  }}
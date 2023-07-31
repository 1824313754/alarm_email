package org.example

import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Properties
import javax.mail.{Message, Session}
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.net.ssl.{HttpsURLConnection, SSLContext, SSLSession, TrustManager, X509TrustManager}

object SendEmail {
  def sendEmail(from: String, password: String, to: List[String], host: String, port: String, subject: String, content: String): Unit = {
    // 设置邮件属性
    // 自定义信任所有证书的TrustManager
    val trustAllManager = new X509TrustManager() {
      override def getAcceptedIssuers: Array[X509Certificate] = null
      override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
      override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
    }
    // 创建信任所有证书的SSLContext
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, Array[TrustManager](trustAllManager), new SecureRandom())
    // 设置信任所有证书的属性
    val properties = new Properties()
    properties.put("mail.smtp.host", host)
    properties.put("mail.smtp.port", port)
    properties.put("mail.smtp.auth", "true")
    properties.put("mail.smtp.starttls.enable", "true")
    properties.put("mail.smtp.ssl.enable", "true")
    properties.put("mail.smtp.ssl.socketFactory", sslContext.getSocketFactory)
    // 创建会话
    val session = Session.getDefaultInstance(properties)

    try {
      // 创建邮件消息
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(from))
      to.foreach(email => message.addRecipient(Message.RecipientType.TO, new InternetAddress(email)))
      message.setSubject(subject)
      message.setText(content)

      // 发送邮件
      val transport = session.getTransport("smtp")
      transport.connect(host, from, password)
      transport.sendMessage(message, message.getAllRecipients)
      transport.close()

      println("邮件发送成功")
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        println("邮件发送失败")
    }
  }



}

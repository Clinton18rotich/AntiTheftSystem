package com.antitheft.app.managers

import android.content.Context
import android.telephony.SmsManager
import com.antitheft.app.utils.Constants
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import javax.mail.*
import javax.mail.internet.*

class DataManager(private val context: Context) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun sendSMS(number: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(number, null, parts, null, null)
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    fun sendEmail(subject: String, body: String, recipient: String) {
        scope.launch {
            try {
                val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                val email = prefs.getString(Constants.PREF_EMAIL, "") ?: ""
                val password = prefs.getString(Constants.PREF_EMAIL_PASSWORD, "") ?: ""
                
                if (email.isEmpty() || password.isEmpty()) {
                    sendSMS(recipient, body)
                    return@launch
                }
                
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "465")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.socketFactory.port", "465")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                }
                
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(email, password)
                    }
                })
                
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(email))
                    setRecipient(Message.RecipientType.TO, InternetAddress(recipient))
                    this.subject = subject
                    setText(body)
                }
                Transport.send(message)
            } catch (e: Exception) {
                sendSMS(recipient, body)
            }
        }
    }
    
    fun sendEmailWithAttachment(subject: String, body: String, file: File, recipient: String) {
        scope.launch {
            try {
                val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                val email = prefs.getString(Constants.PREF_EMAIL, "") ?: ""
                val password = prefs.getString(Constants.PREF_EMAIL_PASSWORD, "") ?: ""
                
                if (email.isEmpty() || password.isEmpty()) {
                    sendSMS(recipient, "File: ${file.name} (${file.length()} bytes)")
                    return@launch
                }
                
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "465")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.socketFactory.port", "465")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                }
                
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(email, password)
                    }
                })
                
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(email))
                    setRecipient(Message.RecipientType.TO, InternetAddress(recipient))
                    this.subject = subject
                    val multipart = MimeMultipart()
                    val bodyPart = MimeBodyPart().apply { setText(body) }
                    multipart.addBodyPart(bodyPart)
                    val attachmentPart = MimeBodyPart().apply { attachFile(file) }
                    multipart.addBodyPart(attachmentPart)
                    setContent(multipart)
                }
                Transport.send(message)
                file.delete()
            } catch (e: Exception) {
                sendSMS(recipient, "Failed to send email. File: ${file.name}")
            }
        }
    }
}

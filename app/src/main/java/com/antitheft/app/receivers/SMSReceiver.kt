package com.antitheft.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.antitheft.app.managers.CommandManager
import com.antitheft.app.managers.CustomCommandManager
import com.antitheft.app.utils.Constants
import com.antitheft.app.utils.CryptoUtils

class SMSReceiver : BroadcastReceiver() {
    
    private lateinit var commandManager: CommandManager
    private lateinit var customCommandManager: CustomCommandManager
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        commandManager = CommandManager(context)
        customCommandManager = CustomCommandManager(context)
        
        val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } else {
            emptyArray()
        }
        
        for (message in messages) {
            val sender = message.originatingAddress ?: continue
            var messageBody = message.messageBody ?: continue
            
            if (!isAuthorized(sender, context)) continue
            
            if (messageBody.startsWith(Constants.ENCRYPTION_PREFIX)) {
                messageBody = CryptoUtils.decrypt(messageBody)
            }
            
            val handled = customCommandManager.processIncomingCommand(sender, messageBody)
            
            if (!handled && messageBody.startsWith(Constants.COMMAND_PREFIX)) {
                commandManager.processCommand(sender, messageBody, context)
            }
            
            abortBroadcast()
        }
    }
    
    private fun isAuthorized(sender: String, context: Context): Boolean {
        val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val authorizedNumbers = prefs.getStringSet(Constants.PREF_AUTH_NUMBERS, emptySet()) ?: emptySet()
        if (authorizedNumbers.contains(sender)) return true
        val senderLast10 = sender.takeLast(10)
        return authorizedNumbers.any { it.takeLast(10) == senderLast10 }
    }
}

package com.x.acs.utility;

import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import static com.x.acs.ACSParserConstants.*;

public class SendMail
{

	private static Properties fMailServerConfig = new Properties();
    

    public SendMail(Properties prop)
    {
        fMailServerConfig.setProperty(EMAIL_HOST, prop.getProperty(EMAIL_HOST));
        fMailServerConfig.setProperty(EMAIL_FROM, prop.getProperty(EMAIL_FROM));
    }

    public void sendEmail(String aSubject, String aBody, String strAttachment, String strMailToUser, String strCC, String strMailFromUser )
        throws MessagingException
    {
    	
    	Session session = Session.getDefaultInstance(fMailServerConfig, null);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(strMailFromUser));
        String [] strRecipients = strMailToUser.split(";");
        for(int i = 0; i < strRecipients.length; i++)
            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(strRecipients[i]));
        strRecipients = strCC.split(";");
        for(int i = 0; i < strRecipients.length; i++)
            message.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(strRecipients[i]));
        message.setSubject(aSubject);
        Multipart mp = new MimeMultipart();
        MimeBodyPart mbpt = new MimeBodyPart();
        MimeBodyPart mbpa = new MimeBodyPart();
        mbpt.setText(aBody);
        mp.addBodyPart(mbpt);
        if(strAttachment != null)
        {
            FileDataSource fds = new FileDataSource(strAttachment);
            mbpa.setDataHandler(new DataHandler(fds));
            mbpa.setFileName(fds.getName());
            mp.addBodyPart(mbpa);
        }
        message.setContent(mp);
        Transport.send(message);
    }

    

}

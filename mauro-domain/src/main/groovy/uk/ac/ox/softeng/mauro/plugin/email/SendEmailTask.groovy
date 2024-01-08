package uk.ac.ox.softeng.mauro.plugin.email

class SendEmailTask implements Runnable {

    Map<String, String> to = [:]
    Map<String, String> cc = [:]
    String body
    String subject
    String fromName
    String fromAddress
    //EmailProviderService emailProviderService
    //EmailService emailService
    String result



    @Override
    void run() {

    }
}

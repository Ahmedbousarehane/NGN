package sample;/* Created by Oussama on 13/01/2018. */

import RTP.Receive;
import RTP.Send;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.Dialog;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

class SipClient implements SipListener {


    private ControllerHome controllerHome;

    // Objets utiles pour communiquer avec l’API JAIN SIP.
    SipFactory sipFactory;            // Pour acceder à l’API SIP.
    SipStack sipStack;                // Le SIP stack.
    SipProvider sipProvider;          // Pour envoyer des messages SIP.
    MessageFactory messageFactory;   // Pour créer les messages SIP.
    HeaderFactory headerFactory;     // Pour créer les entêtes SIP.
    AddressFactory addressFactory;   // Pour créer les SIP URIs.
    ListeningPoint listeningPoint;    // SIP listening IP address/port.
    Properties properties;   //autres propriétés.


    // Objets pour stocker la configuration locale.
    String ip;                         // Adresse IP locale
    int port = (int)(Math.random() * (9000-8000)) + 8000;                  // Port local.
    String protocol = "udp";          // Protocole local de transport (UDP).
    int tag = (new Random()).nextInt();  // Le tag local.
    Address contactAddress;           // L’adresse de contact.
    ContactHeader contactHeader;     // L’entête contact.

    private String transport;
    public SdpFactory sdpFactory;  //pour le corps du message SIP (SDP)

    public SipClient(ControllerHome controllerHome) {
        this.controllerHome = controllerHome;
        ControllerInvite.sipClient = this;
    }


    public void onOpen(javafx.scene.control.TextField localAdr) {
        // A method called when you open your application.
        try {
            // Obtenir l’adresse IP locale.
            this.ip = InetAddress.getLocalHost().getHostAddress();
            // Créer le SIP factory et affecter le path name.
            this.sipFactory = SipFactory.getInstance();
            this.sipFactory.setPathName("gov.nist");
            // Créer et configurer les propriétés du SIP stack
            this.properties = new Properties();
            this.properties.setProperty("javax.sip.STACK_NAME", "stack");
            // Créer le SIP stack.
            this.sipStack = this.sipFactory.createSipStack(this.properties);
            // Créer le message factory de SIP.
            this.messageFactory = this.sipFactory.createMessageFactory();
            // Créer le header factory de SIP.
            this.headerFactory = this.sipFactory.createHeaderFactory();
            // Créer l’address factory de SIP.
            this.addressFactory = this.sipFactory.createAddressFactory();
            // Créer le SIP listening point et le lier à l’adresse IP locale, le port et
            //le protocole.

            this.listeningPoint = this.sipStack.createListeningPoint(this.ip, this.port, this.protocol);

            // Créer le SIP provider.
            this.sipProvider = this.sipStack.createSipProvider(this.listeningPoint);

            // Ajouter cette application comme SIP listener.
            this.sipProvider.addSipListener(this);

            // Créer l’adresse de contacte utilisée pour tous les messages SIP.
            this.contactAddress = this.addressFactory.createAddress("sip:" + this.ip + ":" + this.port);
            // Créer le contact header utilisé pour tous les messages SIP.
            this.contactHeader = this.headerFactory.createContactHeader(contactAddress);

            // Afficher l’adresse IP locale et le port dans le text area.
            localAdr.setText("sip:" + this.ip + ":" + this.port);

        } catch (Exception e) {
            // Affichage de l’erreur
        }

    }




    public void onInvite(javafx.scene.control.TextField destadr) {
        try {
            // Créer le To Header
            // Obtenir l’adresse de destination à partir du text field.
            Address addressTo = this.addressFactory.createAddress(destadr.getText());
            addressTo.setDisplayName("Alice");
            //par exemple
            ToHeader toHeader = headerFactory.createToHeader(addressTo, null);
            // Créer le request URI pour les messages SIP.
            javax.sip.address.URI requestURI = addressTo.getURI();
            // Affecter le type du protocole de Transport TCP ou UDP??
            transport = "udp";
            // Créer les Via Headers
            ArrayList viaHeaders = new ArrayList();
            ViaHeader viaHeader = this.headerFactory.createViaHeader(this.ip, this.port, transport, null);
            // ajouter les via headers
            viaHeaders.add(viaHeader);
            // Créer le ContentTypeHeader
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");

            // Créer une nouvelle entête  CallId
            CallIdHeader callIdHeader = sipProvider.getNewCallId();

            // Créer une nouvelle entête Cseq
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);

            // Créer une nouvelle entête MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

            // Créer le  "From" header.
            FromHeader fromHeader = this.headerFactory.createFromHeader(this.contactAddress, String.valueOf(this.tag));
            // Créer la requête Invite.
            Request request = messageFactory.createRequest(requestURI, Request.INVITE,
                    callIdHeader, cSeqHeader, fromHeader,
                    toHeader, viaHeaders, maxForwards);
            // Ajouter l’adresse de contacte.
            contactAddress.setDisplayName("Messaoudi");

            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);
            // Créer la transaction client.
            ClientTransaction inviteTid = this.sipProvider.getNewClientTransaction(request);

            // envoyer la requête
            inviteTid.sendRequest();

            String sdpData = createSDPData(50002, 0); //create SDP content
            request.setContent(sdpData, contentTypeHeader);


            // Afficher le message dans le text area.
            System.out.println("InviteRequest sent:\n" + request.toString() + "\n\n");
            ChangeWindows.incomingCall(destadr.getText(), true);
            /*
            ControllerHome.send=new Send(destadr.getText().split(":")[1]);
            ControllerHome.send.open();
            ControllerHome.send.start();*/

        } catch (Exception e) {
            //Afficher l’erreur en cas de problème.
            System.out.println("InviteRequest sent failed: " + e.getMessage() + "\n");
        }



    }
    public String createSDPData(int localBasePort, int remoteBasePort) {
        try {
            sdpFactory = SdpFactory.getInstance();

            SessionDescription sessDescr = sdpFactory.createSessionDescription();
            String myIPAddr;
            try {
                myIPAddr = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException Uhe) {
                myIPAddr = "127.0.0.1";
            }       //"v=0"
            Version v = sdpFactory.createVersion(0);
//o=
            Origin o = sdpFactory.createOrigin("1234", 0, 0, "IN", "IP4", myIPAddr);
//"s=-"
            SessionName s = sdpFactory.createSessionName("-");
//c=
            Connection c = sdpFactory.createConnection("IN", "IP4", myIPAddr);
//"t=0 0"
            TimeDescription t = sdpFactory.createTimeDescription();
            Vector timeDescs = new Vector();
            timeDescs.add(t);

            // -------- Description du media Audio
            String[] formats = {"0", "4", "18"};
            MediaDescription am = sdpFactory.createMediaDescription("audio", localBasePort, 1, "RTP/AVP", formats);
//"m=video 22222 RTP/AVP 34";
            String[] vformats = {"34"};
            MediaDescription vm = sdpFactory.createMediaDescription("video", remoteBasePort, 1, "RTP/AVP", vformats);
            Vector mediaDescs = new Vector();

            mediaDescs.add(am);
            mediaDescs.add(vm);
            sessDescr.setVersion(v);
            sessDescr.setOrigin(o);
            sessDescr.setConnection(c);
            sessDescr.setSessionName(s);
            sessDescr.setTimeDescriptions(timeDescs);

            if (mediaDescs.size() > 0) {
                sessDescr.setMediaDescriptions(mediaDescs);
            }

            return sessDescr.toString();
        } catch (SdpException exc) {
            System.out.println("An SDP exception occurred while generating sdp description");
            exc.printStackTrace();
        }
        return "No SDP set";
    }


    public void processRequest(RequestEvent requestEvent) {

        // Get the request.
        Request request = requestEvent.getRequest();
        String descDest = ((FromHeader)request.getHeader("From")).getAddress().toString();
        System.out.println("RECV " + request.getMethod() + " " + descDest);
        Response response;
        try {
            // Get or create the server transaction.
            ServerTransaction transaction = requestEvent.getServerTransaction();
            if(null == transaction) {
                transaction = this.sipProvider.getNewServerTransaction(request);
            }

            // Update the SIP message table.
            if(request.getMethod().equals("INVITE")){

                ChangeWindows.incomingCall(descDest, false);
                while (ControllerInvite.isAccepted == null)
                    Thread.sleep(100);
                if(ControllerInvite.isAccepted){
                    System.out.println("Accept Invite");
                    // If the request is an INVITE & we accepted
                    response = this.messageFactory.createResponse(200, request);
                    ((ToHeader)response.getHeader("To")).setTag(String.valueOf(this.tag));
                    response.addHeader(this.contactHeader);
                    transaction.sendResponse(response);
                    System.out.println("SENT " + response.getStatusCode() + " " + response.getReasonPhrase());
                    ControllerHome.send=new Send(descDest.split(":")[1]);
                    ControllerHome.send.open();
                    ControllerHome.send.start();

                }
                else{
                    System.out.println("Decline Invite");
                    return;
                }


            }

            // Process the request and send a response.

            /*if(request.getMethod().equals("REGISTER") || request.getMethod().equals("INVITE") || request.getMethod().equals("BYE")) {
                // If the request is a REGISTER or an INVITE or a BYE.
                response = this.messageFactory.createResponse(200, request);
                ((ToHeader)response.getHeader("To")).setTag(String.valueOf(this.tag));
                response.addHeader(this.contactHeader);
                transaction.sendResponse(response);
                System.out.println(" / SENT " + response.getStatusCode() + " " + response.getReasonPhrase());
            }*/

            else if(request.getMethod().equals("ACK")) {
                System.out.println("**ACK");
            }

        }
        catch(SipException e) {
            System.out.println("ERROR (SIP): " + e.getMessage());
        }
        catch(Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    Dialog dialog;

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        // A method called when you receive a SIP response.
        // Obtenir le message reponse.
        // Get the request.
        Response response = responseEvent.getResponse();
        String descDest = ((FromHeader)response.getHeader("From")).getAddress().toString();
        System.out.println("RECV " + response.getStatusCode() + " " + descDest);
        if(response.getStatusCode() == Response.OK){
            ChangeWindows.controllerInvite.onAccept(null);
            try{
                dialog = responseEvent.getClientTransaction().getDialog();
                Request request = dialog.createAck(((CSeqHeader)response.getHeader("CSeq")).getSeqNumber());
                response.setHeader(contactHeader);
                dialog.sendAck(request);

                ControllerHome.receive=new Receive(this.ip);
                ControllerHome.receive.start();



            }catch(Exception e) {
                e.getStackTrace();
            }
        }
        // Afficher le message réponse dans le text area.
        System.out.println("\nReceived response: " + response.toString());


    }
    public void onBye() {
        try {
            // A method called when you click on the "Bye" button.
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.BYE);

            Request request = this.dialog.createRequest("BYE");
            ClientTransaction transaction = this.sipProvider.getNewClientTransaction(request);
            this.dialog.sendRequest(transaction);
            try{
                ControllerHome.send.close();
            }
            catch (Exception e){

            }
            try {
                ControllerHome.receive.close();
            }
            catch (Exception ex){

            }
            ChangeWindows.goBack();

        } catch (SipException ex) {
            Logger.getLogger(SipClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(SipClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidArgumentException ex) {
            Logger.getLogger(SipClient.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {

    }

    @Override
    public void processIOException(IOExceptionEvent ioExceptionEvent) {

    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {

    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {

    }

}

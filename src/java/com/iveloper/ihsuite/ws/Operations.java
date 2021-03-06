/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iveloper.ihsuite.ws;

import com.iveloper.comprobantes.security.signer.Signer;
import com.iveloper.comprobantes.utils.ArchivoUtils;
import com.iveloper.ihsuite.db.ihOperations;
import com.iveloper.ihsuite.entities.Lot;
import com.iveloper.ihsuite.entities.SuiteDocument;
import com.iveloper.ihsuite.process.DocumentProcessor;
import com.iveloper.ihsuite.utils.LotType;
import com.iveloper.ihsuite.utils.SendFileEmail;
import com.iveloper.ihsuite.utils.SriStatus;
import com.iveloper.ihsuite.ws.responses.DocFacInstance;
import com.iveloper.ihsuite.ws.responses.wsResponse;
import com.iveloper.ihsuite.ws.responses.wsResponseData;
import es.mityc.javasign.pkstore.CertStoreException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

/**
 *
 * @author Alex
 */
@WebService(serviceName = "Operations")
public class Operations {

//    private final String configuration_path = System.getProperty("confPath");
//    private String configuration_path = "C:\\Users\\Alex\\Documents\\Teletronica\\ihsuite_configuration.properties";
    private String configuration_path = "/usr/local/share/jboss/entities/";
    private String AMBIENTE = "1";

    private ihOperations dbOperations;
    
    private String getConfigurationPath(String entityId){
        return configuration_path + entityId + ".properties";
    }

    /**
     * Crea un Lote para enviar documentos
     *
     * @param description Una descripción general del lote, para referencia o
     * control, por ejemplo: el mes, una fecha, la semana, el grupo de clientes
     * que se va a armar el lote.
     * @param typeLot El tipo de Lote.
     * @param user Usuario del Portal.
     * @param pass Password del usuario indicado.
     * @param userEntityId ID (uuid) de la empresa del usuario indicado.
     * @param notiEmailInternal Email para notificación interna de lo que sucede
     * con los documentos del lote, tambien conocido como email del Operador o
     * del usuario que esta procesando el documento o el lote.
     * @return Devuelve un objecto de tipo wsResponse, con 2 valores
     * principales: LotId: Id (uuid) interno del Lote y LotNumber: Número del
     * Lote creado.
     */
    @WebMethod(operationName = "CreateLot")
    public wsResponse CreateLot(@WebParam(name = "description") String description, @WebParam(name = "typeLot") LotType typeLot, @WebParam(name = "user") String user, @WebParam(name = "pass") String pass, @WebParam(name = "userEntityId") String userEntityId, @WebParam(name = "notiEmailInternal") String notiEmailInternal) {
        wsResponse res = new wsResponse();
        try {
            dbOperations = new ihOperations(getConfigurationPath(userEntityId));
//            dbOperations = new ihOperations(configuration_path);
            dbOperations.setDatabase(userEntityId);
            dbOperations.setUser(user);
            dbOperations.setPass(pass);
            dbOperations.connect();
            Lot lot = dbOperations.createLot(typeLot);
            dbOperations.disconnect();
            res.setProcessed(true);
            res.setLotId(lot.getLotId().toString());
            res.setLotNumber(lot.getLotNumber().toString());
        } catch (IOException | ClassNotFoundException | SQLException ex) {
            Logger.getLogger(Operations.class.getName()).log(Level.SEVERE, null, ex);
            res.setProcessed(false);
            res.setMessageException(Arrays.asList(ex.toString()));
        }
        if (notiEmailInternal != null) {
            SendFileEmail sfe = new SendFileEmail();
            sfe.setUser("alex@iveloper.com");
            sfe.setPwd("bgNILL1982");
            sfe.setHost("mail.iveloper.com");
            sfe.setFrom("alex@iveloper.com");
            sfe.setTo(notiEmailInternal);
            sfe.setMessageSubject("InvoiceHUB Suite");
            if (res.isProcessed()) {
                sfe.setMessageBody("Se creo Lote " + res.getLotId());
            } else {
                sfe.setMessageBody("Falló creación de Lote");
            }
            Thread thread2 = new Thread(sfe);
            thread2.start();
        }
        return res;
    }

    /**
     * Permite enviar un string XML, con un adjunto PDF (en bytes)
     * opcionalmente, especificando el tipo de aplicacion que envia el
     * documento. docAppRefCode, este parámetro sirve cuando una empresa tiene
     * diferentes sistemas (ERP) para generar los documentos de venta, y asi
     * poder indentificar que software es el que generó dicho documento.
     * Incluso, se puede configurar parámetros específicos para un tipo de
     * documento y software (segun este campo).
     *
     * @param lotId Id (uuid) del lote, obtenido previamente por el metodo
     * CreateLot.
     * @param docAppRefCode Codigo o referencia del tipo de aplicación/software.
     * @param xmlString La cadena de texto XML.
     * @param docTypeCode El tipo de documento segun la codificación del SRI.
     * @param docNum El número del documento en formato ###-###-#########
     * ejemplo: 001-001-000000305.
     * @param referencia Una referencia o glosa general del documento.
     * @param docRef El documento PDF a adjuntar en Bytes.
     * @param sendNotification True o False Determina si procesa o no la
     * notificación al receptor, si es False solo procesa hasta obtener la
     * autorización del documento.
     * @param user Usuario del Portal
     * @param clientContact Objeto que contiene los datos del contacto a quien
     * entregar el documento
     * @param pass Password del usuario indicado
     * @param userEntityId ID (uuid) de la empresa del usuario indicado
     * @return Devuelve un objecto de tipo wsResponse, con 1 valor principal:
     * DoumentId: Id (uuid) interno del documento.
     */
    @WebMethod(operationName = "AddFilesWithApp")
    public wsResponse AddFilesWithApp(@WebParam(name = "lotId") String lotId, @WebParam(name = "docAppRefCode") String docAppRefCode, @WebParam(name = "xmlString") String xmlString, @WebParam(name = "docTypeCode") String docTypeCode, @WebParam(name = "docNum") String docNum, @WebParam(name = "referencia") String referencia, @WebParam(name = "docRef") byte[] docRef, @WebParam(name = "sendNotification") boolean sendNotification, @WebParam(name = "clientContact") com.iveloper.ihsuite.utils.ClientContactObject clientContact, @WebParam(name = "user") String user, @WebParam(name = "pass") String pass, @WebParam(name = "userEntityId") String userEntityId) {
        wsResponse res = new wsResponse();
        res.setLotId(lotId);

        Logger.getLogger(Operations.class.getName()).log(Level.INFO, "Contenido xml: {0}", xmlString);

        try {
            dbOperations = new ihOperations(getConfigurationPath(userEntityId));
//            dbOperations = new ihOperations(configuration_path);
            dbOperations.setDatabase(userEntityId);
            dbOperations.setUser(user);
            dbOperations.setPass(pass);
            dbOperations.connect();
            InputStream is = ArchivoUtils.xmlStringToInputStream(xmlString);
            String validation = ArchivoUtils.validarContraEsquema(dbOperations, docTypeCode, is);
            if (validation == null) {
                Logger.getLogger(Operations.class.getName()).log(Level.INFO, "El archivo tiene una construcción válida.");
                Signer signer = new Signer(ArchivoUtils.xmlStringToInputStream(xmlString), dbOperations.getDigitalCertificate(userEntityId));
                ByteArrayOutputStream bos = signer.sign();
                Logger.getLogger(Operations.class.getName()).log(Level.INFO, "Se firmó el documento");
                SuiteDocument sdoc = new SuiteDocument();
                sdoc.setLotId(UUID.fromString(lotId));
//                sdoc.setLotId(UUID.randomUUID());
                sdoc.setBos(bos);
                sdoc.setDocNum(docNum);
                sdoc.setDocTypeCode(docTypeCode);
                sdoc.setDocAppRefCode(docAppRefCode);
                sdoc.setDocRef(docRef);
                sdoc.setReference(referencia);
                sdoc.setStatus("NO PROCESADO");
                if (clientContact != null) {
                    sdoc.setNotifyname(clientContact.getName());
                    sdoc.setNotifyemail(clientContact.getEmail1());
                }
                if (dbOperations.createSuiteDocument(sdoc) == 1) {
                    Logger.getLogger(Operations.class.getName()).log(Level.INFO, "Se guardó el documento");
                    res.setProcessed(true);
                    res.setDocumentId(sdoc.getDocumentId().toString());
                    DocumentProcessor docprocessor = new DocumentProcessor();
                    docprocessor.setAmbiente(AMBIENTE);
                    docprocessor.setDbOperations(dbOperations);
                    docprocessor.setSdoc(sdoc);

                    Thread thread1 = new Thread(docprocessor);
                    thread1.start();
                }
            } else {
                Logger.getLogger(Operations.class.getName()).log(Level.WARNING, "El archivo est\u00e1 mal construido o est\u00e1 vacio: {0}", validation);
                res.setProcessed(false);
                res.setMessageException(Arrays.asList("El archivo está mal construido o está vacio: " + validation));
            }
            dbOperations.disconnect();

        } catch (IOException | ClassNotFoundException | SQLException | SAXException | TransformerException | ParserConfigurationException | CertificateException | CertStoreException ex) {
            Logger.getLogger(Operations.class.getName()).log(Level.SEVERE, null, ex);
            res.setProcessed(false);
            res.setMessageException(Arrays.asList(ex.toString()));
        } catch (Exception ex) {
            Logger.getLogger(Operations.class.getName()).log(Level.SEVERE, null, ex);
            res.setProcessed(false);
            res.setMessageException(Arrays.asList(ex.toString()));
        }
        Logger.getLogger(Operations.class.getName()).log(Level.INFO, "Finalizó llamada a operación.");
        if (sendNotification) {
            SendFileEmail sfe = new SendFileEmail();
            sfe.setUser("alex@iveloper.com");
            sfe.setPwd("bgNILL1982");
            sfe.setHost("mail.iveloper.com");
            sfe.setFrom("alex@iveloper.com");
            sfe.setTo("alexfbonilla@hotmail.com");
            sfe.setMessageSubject("InvoiceHUB Suite");
            if (res.isProcessed()) {
                sfe.setMessageBody("Se creó el documento " + res.getDocumentId());
            } else {
                sfe.setMessageBody("Falló la creación de documento");
            }

            Thread thread2 = new Thread(sfe);
            thread2.start();
        }

        return res;
    }

    /**
     * Obtiene los datos inherentes a los posibles estados e instancias del
     * documento.
     *
     * @param documentId Id (uuid) del documento en InvoiceHub, se obtuvo en
     * cualquiera de los metodos Add
     *
     * @param user Usuario del Portal
     * @param pass Password del usuario indicado
     * @param userEntityId ID (uuid) de la empresa del usuario indicado
     * @return Devuelve un objecto de tipo wsResponseData, con los datos
     * correspondientes si encuentra la clave especificada.
     */
    @WebMethod(operationName = "GetData")
    public wsResponseData GetData(@WebParam(name = "documentId") String documentId, @WebParam(name = "user") String user, @WebParam(name = "pass") String pass, @WebParam(name = "userEntityId") String userEntityId) {
        wsResponseData wres = new wsResponseData();
        wres.setDocumentId(documentId);
        wres.setProcessed(false);
        try {
            dbOperations = new ihOperations(getConfigurationPath(userEntityId));
//            dbOperations = new ihOperations(configuration_path);
            dbOperations.setDatabase(userEntityId);
            dbOperations.setUser(user);
            dbOperations.setPass(pass);
            dbOperations.connect();
            DocFacInstance document = dbOperations.getDocFacInstance(documentId);
            if (document != null) {
                wres.setProcessed(true);
                wres.setDocumentStatus(document.getDocstatus());
                wres.setDocumentInstance(document);
                if (document.getDocstatus() == SriStatus.Autorizada) {
                    wres.setDocumentAutorizacion(document.getDocautorizacion());
                    wres.setDocumentAutorizacionDate(document.getDocautorizaciondate());
                    Logger.getLogger(Operations.class.getName()).log(Level.INFO, "No. de Autorización obtenido: {0}", wres.getDocumentAutorizacion());
                    Logger.getLogger(Operations.class.getName()).log(Level.INFO, "Fecha de Autorización: {0}", wres.getDocumentAutorizacionDate());
                }
            }
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            Logger.getLogger(Operations.class.getName()).log(Level.SEVERE, null, ex);
        }
        return wres;
    }

    /**
     * Confirmación, si exite un documento especificado en la plataforma.
     *
     * @param docTypeCode El tipo de documento segun la codificación del SRI.
     * @param docNum El número del documento en formato ###-###-#########
     * ejemplo: 001-001-000000305.
     * @param user Usuario del Portal
     * @param pass Password del usuario indicado
     * @param userEntityId ID (uuid) de la empresa del usuario indicado
     * @return Devuelve un objecto de tipo wsResponse, con 1 valor principal:
     * DoumentId: Id (uuid) interno del documento.
     */
    @WebMethod(operationName = "DocumentExist")
    public wsResponse DocumentExist(@WebParam(name = "docTypeCode") String docTypeCode, @WebParam(name = "docNum") String docNum, @WebParam(name = "user") String user, @WebParam(name = "pass") String pass, @WebParam(name = "userEntityId") String userEntityId) {
        //TODO write your implementation code here:
        return null;
    }

    /**
     * Cerrar un lote, para no permitir agregar mas archivos via los metodos
     * Add*.
     *
     * @param lotId Id (uuid) del Lote obtenido en CreateLote
     * @param user Usuario del Portal
     * @param pass Password del usuario indicado
     * @param userEntityId ID (uuid) de la empresa del usuario indicado
     * @return Devuelve un objecto de tipo wsResponse, con 2 valores
     * principales: LotId: Id (uuid) interno del LoteLotNumber: Número del Lote
     * creado.
     */
    @WebMethod(operationName = "CloseLot")
    public wsResponse CloseLot(@WebParam(name = "lotId") String lotId, @WebParam(name = "user") String user, @WebParam(name = "pass") String pass, @WebParam(name = "userEntityId") String userEntityId) {
        //TODO write your implementation code here:
        return null;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.iveloper.ihsuite.ws.responses;

/**
 *
 * @author Alex
 */
public class wsResponse extends wsResponseBase{
    private String DocumentId;
    private String LotId;
    private String LotNumber;

    public String getDocumentId() {
        return DocumentId;
    }

    public void setDocumentId(String DocumentId) {
        this.DocumentId = DocumentId;
    }

    public String getLotId() {
        return LotId;
    }

    public void setLotId(String LotId) {
        this.LotId = LotId;
    }

    public String getLotNumber() {
        return LotNumber;
    }

    public void setLotNumber(String LotNumber) {
        this.LotNumber = LotNumber;
    }
    
}

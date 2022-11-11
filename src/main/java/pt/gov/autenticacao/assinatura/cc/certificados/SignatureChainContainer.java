/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.gov.autenticacao.assinatura.cc.certificados;

import pt.gov.autenticacao.common.AbstractContainer;
import pt.gov.autenticacao.common.ContainerException;
import pt.gov.autenticacao.common.Input;
import pt.gov.autenticacao.common.Inputs;
import pt.gov.autenticacao.common.ReturnParameter;

/**
 *
 * @author ruim
 */
public class SignatureChainContainer extends AbstractContainer {
    private byte[] certificado = null;
    private byte[] certificadoSubEC = null;

    public void setCertificado(byte[] certificado) {
        this.certificado = certificado;
    }

  
    public void setCertificadoSubEC(byte[] certificadoSubEC) {
        this.certificadoSubEC = certificadoSubEC;
    }
    
    
    public Inputs getInputs() throws ContainerException {
        Inputs inputs = new Inputs();
                                
        inputs.addInput(new Input(ReturnParameter.CERTIFICADO, certificado));
        inputs.addInput(new Input(ReturnParameter.CERTIFICADOEC, certificadoSubEC));
        inputs.addInput(new Input(ReturnParameter.NONCE, token));
        
        return inputs;
    }
    
    
}

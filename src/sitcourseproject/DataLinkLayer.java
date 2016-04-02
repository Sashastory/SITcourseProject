/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sitcourseproject;

import gnu.io.*;
import java.io.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
/**
 *
 * @author Александр
 */
public class DataLinkLayer {
    private SerialPort serialPort;
    public boolean isConnect;
    private final SerialPortProject physicalLayer;
    private final NewJFrame window;
    private OutputStream output;
    
    private byte[] portParameters = new byte[4];
    
    private boolean dataFlag = false;
    private boolean isMaster = false;
    private boolean isReceived = false;
    public boolean isReady = true;
    
    
    private boolean connectionFlag = false;
    private boolean ackFlag = false;
    private boolean setFlag = false;
    private boolean paramFlag = false;
    
    int globalBufIndex2 = 0;
    int sum2 = 0;
    byte[] globalBuffer = new byte[100000];
    
    public DataLinkLayer(NewJFrame window) {
        this.physicalLayer = new SerialPortProject(window,this);
        this.window = window;
        this.dataFlag = true;
        this.isReady =true;
    }
    
    public SerialPortProject getPhysicalLayer() {
        return physicalLayer;
    }
    
    public static Integer[] toBitArray(byte b) {
        String str = Integer.toBinaryString(b);
        Integer[] bitArray = new Integer[str.length()];
        Integer[] result = {0,0,0,0,0,0,0,0};
        for (int i=0; i<bitArray.length; i++) {
            bitArray[i]= str.charAt(i)=='1' ? 1 : 0;
        }
        if(bitArray.length != 8) {
            System.arraycopy(bitArray,0,result,8-bitArray.length,bitArray.length); 
            return result;
        }
        return bitArray;
    }
    
    public byte[] createLinkFrame() {
        byte[] frame = new byte[130];
        frame[0] = (byte)0xFF;
        frame[1] = (byte)0x41;
        frame[129] = (byte)0xFF;
        return frame;
    }
    
    public byte[] createInformFrame(byte[] data) {
        byte[] frame = new byte[130];
        frame[0] = (byte)0xFF;
        frame[1] = (byte)0x48;
        frame[2] = (byte) data.length;
        for (int i=3; i < data.length+3; i++) {
            frame[i] = data[i-3];
        }
        frame[129] = (byte)0xFF;
        return frame;
    }
    
    public byte[] createSetFrame(byte[] data) {
        byte[] frame = new byte[130];
        frame[0] = (byte)0xFF;
        frame[1] = (byte)0x47;
        for (int i=2; i < 6; i++) {
            frame[i] = data[i-2];
        }
        frame[129] = (byte)0xFF;
        return frame;
    }
    
    public byte[] createAckFrame() {
        byte[] frame = new byte[130];
        frame[0] = (byte)0xFF;
        frame[1] = (byte)0x54;
        frame[129] = (byte)0xFF;
        return frame;
    }
    
    public byte[] createNakFrame() {
        byte[] frame = new byte[130];
        frame[0] = (byte)0xFF;
        frame[1] = (byte)0x46;
        frame[129] = (byte)0xFF;
        return frame;
    }
    
    public byte[] createDisconnectFrame() {
        byte[] frame = new byte[130];
        frame[0] = (byte)0xFF;
        frame[1] = (byte)0x44;
        frame[129] = (byte)0xFF;
        return frame;
    }
    
    public void receiveLinkFrame() {
        window.jTextArea1.append("Link кадр получен" + "\n");
        this.isMaster = false;
        this.ackFlag = true;
        byte[] ackFrame = createAckFrame();
        this.physicalLayer.writeRawBits(ackFrame);
    }
    
    public void receiveInformFrame(byte[] data) {
        window.jTextArea1.append("Inform кадр получен" + "\n");
        try {
            if(!isReceived) {
                JOptionPane.showMessageDialog(window, "Выберите путь для сохранения файла");
                JFileChooser  saveFile = new JFileChooser();
                saveFile.showSaveDialog(null);
                String filePath = saveFile.getSelectedFile().getAbsolutePath();
                output = new FileOutputStream(filePath);
                isReceived = true;
            }
            int infLength = data[2];
            int curPos = 0;
            byte[] filePartCoded = new byte[infLength];
            byte[] filePart = new byte[infLength/14];
            byte[] codedByte = new byte[14];
            System.arraycopy(data,3,filePartCoded,0,infLength);
            for(int frameIndex=0; frameIndex < infLength; frameIndex+=14,curPos++) {
                System.arraycopy(filePartCoded,frameIndex,codedByte,0,14);
                byte temp = Encrypter.decoding(codedByte);
                filePart[curPos] = temp;
            }
            output.write(filePart,0,filePart.length);
            byte[] ackFrame = createAckFrame();
            this.physicalLayer.writeRawBits(ackFrame);
        } catch(FileNotFoundException fnfe) {
        } catch(IOException ex) {
        }
        
    }
    
    public void receiveSetFrame(byte[] readBuffer) {
        window.jTextArea1.append("Set кадр получен" + "\n");
        this.setFlag = true;
        byte[] ackFrame = createAckFrame();
        this.physicalLayer.writeRawBits(ackFrame);
        for(int i = 0;i<4;i++) {
            this.portParameters[i] = readBuffer[i+2];
        }
        
    }
    
    public void receiveAckFrame() {
        window.jTextArea1.append("Ack кадр получен" + "\n");
        if(!ackFlag && !connectionFlag) {
            byte[] ackReply = createAckFrame();
            this.physicalLayer.writeRawBits(ackReply);
            ackFlag = true;
            connectionFlag = true;
            window.jTextArea1.append("Станция назначена мастером" + "\n\n");
            window.getConnectButton().setEnabled(!isMaster);
        } else if(ackFlag && !connectionFlag) {
            connectionFlag = true;
            window.jTextArea1.append("Соединение успешно установлено" + "\n");
            window.jTextArea1.append("Станция назначена слэйвом" + "\n\n");
            window.getConnectButton().setEnabled(isMaster);
            window.getSetParamsButton().setEnabled(isMaster);
        } else if(!setFlag && !paramFlag) {
            byte[] ackReply = createAckFrame();
            this.physicalLayer.writeRawBits(ackReply);
            setFlag = true;
            paramFlag = true;
            this.physicalLayer.setSerialPortParams(portParameters);
            window.jTextArea1.append("Параметры соединения установлены" + "\n\n");
        } else if(setFlag && !paramFlag) {
            paramFlag = true;
            this.physicalLayer.setSerialPortParams(portParameters);
            window.jTextArea1.append("Параметры порта успешно получены" + "\n");
            window.jTextArea1.append("Параметры соединения установлены" + "\n\n");
        } else if(!isReady && connectionFlag && paramFlag) {
            isReady = true;
        }    
    }
    
    public void receiveNakFrame() {
        window.jTextArea1.append("Nak кадр получен" + "\n");
    }
    
    public void receiveDisconnectFrame() {
        window.jTextArea1.append("Disconnect кадр получен" + "\n");
    }
    
    public void initializePortParameters(byte[] params) {
        this.portParameters = params;
        byte[] setFrame = createSetFrame(params);
        this.physicalLayer.writeRawBits(setFrame);
    }
    public void initiateConnection() {
        byte[] linkFrame = createLinkFrame();
        this.physicalLayer.writeRawBits(linkFrame);
        this.isMaster = true;
    }
    
    public void receiveDataFromAppLayer(String fileName) {
        try {
            while(isReady) {
                FileInputStream fis = new FileInputStream(fileName);
                byte[] fileBuffer = new byte[9];
                int numberOfBytes2,frameLength;
                while(fis.available() > 0) {
                    int destPos= 0;
                    numberOfBytes2 = fis.read(fileBuffer);
                    frameLength = numberOfBytes2*14;
                    byte[] frame = new byte[frameLength];
                    Integer[] singleByte;
                    Integer[] firstPart = new Integer[4];
                    Integer[] secondPart = new Integer[4];
                    byte[] firstPartCoded;
                    byte[] secondPartCoded;
                    for(int frameIndex = 0; frameIndex < numberOfBytes2; frameIndex++) {
                        singleByte = toBitArray(fileBuffer[frameIndex]);
                        System.arraycopy(singleByte,0,firstPart,0,4);
                        System.arraycopy(singleByte,4,secondPart,0,4);
                        firstPartCoded = Encrypter.coding(firstPart);
                        secondPartCoded = Encrypter.coding(secondPart);
                        System.arraycopy(firstPartCoded,0,frame,destPos,7);
                        System.arraycopy(secondPartCoded,0,frame,destPos+7,7);
                        destPos+=14;
                    }
                    byte[] informFrame = createInformFrame(frame);
                    this.getPhysicalLayer().writeRawBits(informFrame);
                    isReady = false;
                }    
            }
        } catch (IOException e) {
        }      
    }
    
    public void readDataFromPhysicalLayer(byte[] readBuffer) {
        if (readBuffer[1] == (byte)0x41) {
            receiveLinkFrame();
        } else if(readBuffer[1] == (byte)0x48) {
            receiveInformFrame(readBuffer);
        } else if(readBuffer[1] == (byte)0x47) {
            receiveSetFrame(readBuffer);
        } else if(readBuffer[1] == (byte)0x54) {
            receiveAckFrame();
        } else if(readBuffer[1] == (byte)0x46) {
            receiveNakFrame();
        } else if(readBuffer[1] == (byte)0x44) {
            receiveDisconnectFrame();
        }
    }
}

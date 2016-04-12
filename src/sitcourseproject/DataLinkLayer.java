/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sitcourseproject;

import gnu.io.*;
import java.io.*;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private byte[] globalInformFrame;
    private String fileExtension;
    private String globalFileName;
   
    private boolean dataFlag = false;
    private boolean isMaster = false;
    private boolean isReceived = false;
    public boolean isReady = true;
    
    
    private boolean connectionFlag = false;
    private boolean ackFlag = false;
    private boolean repeatFlag = false;
    private boolean setFlag = false;
    private boolean paramFlag = false;
    
    int globalBufIndex2 = 0;
    int sum2 = 0;
    int repeatCount = 0;
    long diskSpace;
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
        if (str.length() > 8) {
            str = str.substring(24);
        }
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
    
    public byte[] createLengthFrame(byte length) {
        byte[] frame = new byte[130];
        frame[0] = (byte)0xFF;
        frame[1] = (byte)0x49;
        frame[2] = length;
        frame[129] = (byte)0xFF;
        return frame;
    }
    
    public byte[] createNameFrame(byte[] fileName) {
        byte[] frame = new byte[130];
        frame[0] = (byte)0xFF;
        frame[1] = (byte)0x50;
        frame[2] = (byte) fileName.length;
        for (int i=3; i < fileName.length+3; i++) {
            frame[i] = fileName[i-3];
        }
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
    
    public void diskSpace() {
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            try {
                FileStore store = Files.getFileStore(root);
                String rootString = root.toString();
                if (rootString.equals("C:\\")) {
                    diskSpace = store.getUsableSpace();
                }
            } catch (IOException ex) {
            }
        }
    }
    
    public void decodingError() {
        byte[] nakFrame = createNakFrame();
        this.physicalLayer.writeRawBits(nakFrame);
    }
    
    public void receiveLengthFrame(byte length) {
        window.jTextArea1.append("Length кадр получен" + "\n");
        if (length < diskSpace) {
            byte[] ackFrame = createAckFrame();
            this.physicalLayer.writeRawBits(ackFrame);
            window.jTextArea1.append("На диске достаточно места для записи файла" + "\n");
        } else {
            byte[] nakFrame = createNakFrame();
            this.physicalLayer.writeRawBits(nakFrame);
            window.jTextArea1.append("На диске не достаточно места для записи файла" + "\n");
        }
    }
    
    public void receiveNameFrame(byte[] data) {
        window.jTextArea1.append("Name кадр получен" + "\n"); 
        int nameLength = data[2];
        int curPos = 0;
        byte[] filePartCoded = new byte[nameLength];
        byte[] filePart = new byte[nameLength/14];
        byte[] codedByte = new byte[14];
        System.arraycopy(data,3,filePartCoded,0,nameLength);
        for(int frameIndex=0; frameIndex < nameLength; frameIndex+=14,curPos++) {
            System.arraycopy(filePartCoded,frameIndex,codedByte,0,14);
            byte temp = Encrypter.decoding(codedByte);
            filePart[curPos] = temp;
        }
        globalFileName = new String(filePart);
        fileExtension = globalFileName.substring(globalFileName.lastIndexOf('.')+1);
        byte[] ackFrame = createAckFrame();
        this.physicalLayer.writeRawBits(ackFrame);
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
                File file = new File(globalFileName);
                JFileChooser  saveFile = new JFileChooser();
                saveFile.setSelectedFile(file);
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
                if(temp == -1) {
                    decodingError();
                    return;
                }
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
        while(repeatCount < 3) {
           this.physicalLayer.writeRawBits(globalInformFrame);
           window.jTextArea1.append("Inform кадр отправлен повторно" + "\n");
           repeatCount++;
        }
        if (repeatCount == 3) {
            byte[] dscFrame = createDisconnectFrame();
            this.physicalLayer.writeRawBits(dscFrame);
        }
    }
    
    public void receiveDisconnectFrame() {
        window.jTextArea1.append("Disconnect кадр получен" + "\n"
        + "Разрыв соединения");
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
    
    public void sendLengthOfFile(String fileName) {
        File file = new File(fileName);
        byte fileLength = (byte) file.length();
        byte[] lengthFrame = createLengthFrame(fileLength);
        this.physicalLayer.writeRawBits(lengthFrame);
    }
    
    public void sendFileName(String fileNameFull) {
        String fileName = fileNameFull.substring(fileNameFull.lastIndexOf("\\")+1);
        int bufSize = fileName.length();
        byte[] stringBuffer = new byte[bufSize];
        stringBuffer = fileName.getBytes();
        int destPos = 0;
        int amountOfBytes = bufSize*14;
        byte[] frame = new byte[amountOfBytes];
        Integer[] singleByte;
        Integer[] firstPart = new Integer[4];
        Integer[] secondPart = new Integer[4];
        byte[] firstPartCoded;
        byte[] secondPartCoded;
        for(int frameIndex = 0; frameIndex < bufSize; frameIndex++) {
            singleByte = toBitArray(stringBuffer[frameIndex]);
            System.arraycopy(singleByte,0,firstPart,0,4);
            System.arraycopy(singleByte,4,secondPart,0,4);
            firstPartCoded = Encrypter.coding(firstPart);
            secondPartCoded = Encrypter.coding(secondPart);
            System.arraycopy(firstPartCoded,0,frame,destPos,7);
            System.arraycopy(secondPartCoded,0,frame,destPos+7,7);
            destPos+=14;
        }
        byte[] nameFrame = createNameFrame(frame);
        this.getPhysicalLayer().writeRawBits(nameFrame);      
    }
    public void receiveDataFromAppLayer(String fileName) {
        try {
            while(isReady) {
                File file = new File(fileName);
                byte fileLength = (byte) file.length();
                FileInputStream fis = new FileInputStream(file);
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
                    globalInformFrame = createInformFrame(frame);
                    this.getPhysicalLayer().writeRawBits(globalInformFrame);
                    isReady = false;
                }    
            }
        } catch (IOException e) {
        }      
    }
    
    public void readDataFromPhysicalLayer(byte[] readBuffer) {
        if (readBuffer[1] == (byte)0x41) {
            receiveLinkFrame();
        } else if(readBuffer[1] == (byte)0x49) {
            receiveLengthFrame(readBuffer[2]);
        } else if(readBuffer[1] == (byte)0x50) {
            receiveNameFrame(readBuffer);
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

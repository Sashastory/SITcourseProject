/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sitcourseproject;

import gnu.io.*;
import java.io.*;
import java.awt.Color;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
/**
 *
 * @author Александр
 */
public class SerialPortProject implements SerialPortEventListener {
    
    //Главный интерфейс
    NewJFrame window = null;
    
    DataLinkLayer dataLinkLayer;

    //Все найденные порты
    private Enumeration ports = null;
    //Мапа порт - идентификатор
    private HashMap portMap = new HashMap();
    //Объект для открытого порта
    private CommPortIdentifier selectedPortIdentifier = null;
    private SerialPort serialPort = null;

    //Входной и выходной потоки
    private InputStream input = null;
    private OutputStream output = null;

    //Подключены к порту или нет
    private boolean bConnected = false;

    //Таймаут коннекта к порту
    final static int TIMEOUT = 2000;

    //Строка для хранения вывода в окно логов
    String logText = "";
    String test = "";
    //Флаги
    private boolean openFlag = false;
    private boolean isRecieved = false;
    private boolean isReady = false;
    private boolean isInterrupted = false;
    private boolean isOpened = false;
    private boolean isConnected = false;
    private boolean isMaster = false;
    public boolean isExp = false;
    
    private int[] baudRates = new int[]{38400,57600,115200,230400};
    int sum = 0;
    byte[] globalBuffer = new byte[100000];
    int globalBufIndex = 0;
    
    public SerialPortProject(NewJFrame window, DataLinkLayer layer) {
        this.window = window;
        this.dataLinkLayer = layer;
        this.isExp = false;
    }
    
    public SerialPortProject(DataLinkLayer layer) {
        this.dataLinkLayer = layer;
    }
    //Ищем все COM порты
    //pre: none
    //post: добавляет все порты в комбобокс
    public HashMap searchForPorts() {
        ports = CommPortIdentifier.getPortIdentifiers();      
        if (ports == null) {
            System.out.println("COM порты не найдены!");
        } else {
            while (ports.hasMoreElements()) {           
                CommPortIdentifier curPort = (CommPortIdentifier)ports.nextElement();
                //get only serial ports
                if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                //window.cboxPorts.addItem(curPort.getName());
                portMap.put(curPort.getName(), curPort);
                }
            }
        }
        return portMap;
    }  
    //Устанавливаем параметры порта
    public void setSerialPortParams(byte[] portParams) {
        try {
            int baudRate = (int)baudRates[portParams[0]];
            int dataBits = (int)portParams[1];
            int stopBits = (int)portParams[2];
            int parity = (int)portParams[3];
            System.out.println("baud= "+baudRate + " dataBits= " + dataBits+ " stopBits= "+stopBits+" parity="+parity);
            serialPort.setSerialPortParams(baudRate, dataBits, stopBits,parity);
        } catch (UnsupportedCommOperationException e) {
            logText = "Не удалось установить параметры соединения!";
            window.jTextAreaLog.append(logText + "\n");
            window.jTextAreaLog.setForeground(Color.RED);
        }
    }
    //Открываем порт
    public void openSerialPort(String port) {
        selectedPortIdentifier = (CommPortIdentifier)portMap.get(port);
        CommPort commPort;
        try {          
            commPort = selectedPortIdentifier.open("SerialPort", TIMEOUT);
            isOpened = true;
            serialPort = (SerialPort)commPort;
            serialPort.setDTR(true);
            //JOptionPane.showMessageDialog(window, "COM порт определен");
            //Параметры будем брать с меню
//            setSerialPortParams(master);
//            serialPort.setSerialPortParams(57600, SerialPort.DATABITS_8, 
//                    SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            //для GUI
            //setConnection(true);
            setConnected(true);
            //Логи
            logText = "Порт " + port + " успешно открыт";
            window.jTextAreaLog.setForeground(Color.black);
            window.jTextAreaLog.append(logText + "\n");
            isExp = false;
        }
        catch (PortInUseException e) {
            isExp = true;
            logText = "Порт " + port + " уже используется";          
            window.jTextAreaLog.setForeground(Color.RED);
            window.jTextAreaLog.append(logText + "\n");
            //window.getOpenButton().setEnabled(true);
        }
        
    }
    
    //Открываем потоки
    //pre: открытый порт
    //post: потоки
    public boolean initIOStream() {
        //Флаг успешного открытия потоков
        boolean successful = false;
        try {
            input = serialPort.getInputStream();
            output = serialPort.getOutputStream();           
            successful = true;
            return successful;
        }
        catch (IOException e) {
            logText = "I/O Streams failed to open. (" + e.toString() + ")";
            window.jTextAreaLog.setForeground(Color.red);
            window.jTextAreaLog.append(logText + "\n");
            return successful;
        }
    } 
    
    //Добавляет Event Listener, ожидающий прихода данных
    public void initListener() {       
        try {
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
            serialPort.notifyOnBreakInterrupt(true);
            serialPort.notifyOnDSR(true);
            serialPort.notifyOnCarrierDetect(true);
        } catch (TooManyListenersException ex) {
            logText = "Too many listeners";
            window.jTextAreaLog.setForeground(Color.red);
            window.jTextAreaLog.append(logText+"\n");
        }
        
    }    
    
    //Закрывает COM порт
    public void disconnect() {       
        try { 
            input.close();
            output.close();
            serialPort.removeEventListener();
            serialPort.close();
            setConnected(false);
            
            isConnected = false;
            
            logText = "Порт закрыт \n";
            window.jTextAreaLog.setForeground(Color.red);
            window.jTextAreaLog.append(logText+"\n");
            window.jTextFieldStation.setText("");
            window.getOpenButton().setEnabled(true);
            window.getConnectButton().setEnabled(true);
            window.getParamsButton().setEnabled(true);
            window.getFileButton().setEnabled(true);
            window.getSendButton().setEnabled(true);
        } catch (Exception ex) {
            logText = "Ошибка при закрытии порта " + serialPort.getName();
            window.jTextAreaLog.setForeground(Color.red);
            window.jTextAreaLog.append(logText + "\n");        
        }
    }
    
    final public boolean getConnected() {
        return bConnected;
    }
    //Подключились к порту, устанавливаем флаг
    public void setConnected(boolean bConnected) {
        this.bConnected = bConnected;
    }
    
    public boolean getOpenFlag() {
        return openFlag;
    }
    
    public boolean getConnection() {
        return isConnected;
    }
    
    public void setConnection(boolean flag) {
        this.isConnected = flag;
    }
    
    //Сам Event приема данных

    /**
     *
     * @param evt
     */
        @Override
    public void serialEvent(SerialPortEvent evt) {
        switch(evt.getEventType()) {
            case SerialPortEvent.BI:
                System.out.println("BI");
                break;
            case SerialPortEvent.OE:
            case SerialPortEvent.FE:
            case SerialPortEvent.PE:
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            {
                System.out.println("DSR");
                boolean flag = this.serialPort.isDSR();
                if(!this.serialPort.isDSR()) {
                    isConnected = false;
                    dataLinkLayer.isConnect = false;
                    JOptionPane.showMessageDialog(window, "Соединение DSR потеряно");
                    this.disconnect();
//                    if(isOpened) {
//                    this.disconnect();
//                    }
                }
            }
            break;
            case SerialPortEvent.RI:
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                break;
            case SerialPortEvent.DATA_AVAILABLE:
            {
                this.readBitsFromInputStream();
            }
            break;
        }
        
    }
    public void writeData(String fileName) {
        try {
            FileInputStream fis = new FileInputStream(fileName);
            //input = fis;
            byte buf[] = new byte[100000];
            int length;
            while(true) {
                length = fis.read(buf);
                if (length<0) break;
                output.write(buf,0,length);
            }
        } catch (IOException ex) {
            Logger.getLogger(SerialPortProject.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    //Пишем
   public void writeRawBits(byte[] data) {
       try {
//           this.isReady = false;
//           this.dataLinkLayer.isReady = false;
           this.output.write(data);
       } catch (IOException e) {
       }
   }
    //Читаем
    public void readBitsFromInputStream() {
        byte[] readBuffer = new byte[130];
        try {
            int numberOfBytes = 0;
            while (this.input.available() > 0) {
                numberOfBytes = this.input.read(readBuffer);
                //Тестирую
                if(numberOfBytes == 1 && readBuffer[0] == 1) {
                    this.isReady = true;
                    this.dataLinkLayer.isReady = true;
                    this.input.reset();
                }
                //Сумма сдвигается
                this.sum += numberOfBytes;
                //Глобальный буффер и индексы сдвигаются
                for(int i =0;i < numberOfBytes; i++) {
                    this.globalBuffer[this.globalBufIndex] = readBuffer[i];
                    this.globalBufIndex++;
                }
            }
            //Формируются кадры по очереди
            int index = 0;
            while (this.globalBufIndex - 130 >= 0) {
                int frameIndex = 0;
                byte[] frame = new byte[130];
                for(frameIndex = 0; frameIndex < 130; frameIndex++) {
                    frame[frameIndex] = this.globalBuffer[index];
                    index++;
                }
                //Все сбрасывается
                this.sum = 0;
                this.globalBufIndex -= 130;
                this.dataLinkLayer.readDataFromPhysicalLayer(frame);
            }              
        } catch (IOException e) {
        }    
    }
    
    public int[] getBaudRates() {
        return this.baudRates;
    } 
    public OutputStream getOutputStream() {
        return this.output;
    }
    
    public InputStream getInputStream() {
        return this.input;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here 
    }
    
}

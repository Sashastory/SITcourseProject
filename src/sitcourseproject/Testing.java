/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sitcourseproject;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author Alex
 */
public class Testing {
    
    public static Integer[] toBitArray(byte b) {
        String str = Integer.toBinaryString(b);
        if (str.length() > 8) {
            str = str.substring(24);
        }
        Integer[] bitArray = new Integer[8];
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
    
    public static String zeroDeletion(String str) {
        while(str.charAt(0) == '0') {
            str = str.substring(1);
        }
        return str;
    }
    public static String intToStr(Integer[] array1,Integer[] array2) {
        StringBuilder sb = new StringBuilder(8);
        boolean negative = false;
        if(array1[0] == 1) {
            negative = true;
            for(int i =0; i < 4; i++) {
                if(array1[i] == 0) {
                    array1[i] = 1;
                } else {
                    array1[i] = 0;
                }
                if(array2[i] == 0) {
                    array2[i] = 1;
                } else {
                    array2[i] = 0;
                }
            }
            if (array2[3] == 1) {
                array2[3] = 0;
            } else {
                array2[3] = 1;
            }
        }
        for(int i = 0; i < 8; i++) {
            if(i < 4) {
                sb.append(array1[i]);
            } else {
                sb.append(array2[i-4]);
            }
        }
        String result = sb.toString();
        if (negative) {
            return "-" + result;
        } else {
            return result;
        }
    }
    
    public static void main(String[] args) {
        
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            
            System.out.println(root + ": ");
            try {
                FileStore store = Files.getFileStore(root);
                System.out.println("available" + store.getUsableSpace()
                        + ", total = " + store.getTotalSpace());
            } catch (IOException ex) {
            }
        }
    }
}

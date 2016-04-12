package sitcourseproject;

import java.util.ArrayList;
import java.util.List;
import java.nio.*;
import java.util.Arrays;

/**
 * Created by Alex on 31.03.2016.
 */
public class Encrypter {

    private static Integer[] divisor = {1,0,1,1};
    private static List<Integer[]> syndromeList = new ArrayList<Integer[]>() {{
        add(0,new Integer[] {1});
        add(1,new Integer[] {1,0});
        add(2,new Integer[] {1,0,0});
        add(3,new Integer[] {1,1});
        add(4,new Integer[] {1,1,0});
        add(5,new Integer[] {1,1,1});
        add(6,new Integer[] {1,0,1});
    }};

    public static Integer[] division(Integer[] devidend) {
        Integer[] residue = devidend.clone();
        devidend = zeroElementsDeletion(devidend);
        //Больше ли
        if(devidend.length >= divisor.length){
            Integer[] result = new Integer[divisor.length];
            System.arraycopy(devidend,0,result,0,divisor.length);

            while(residue.length >= divisor.length){
                for(int i=0; i < divisor.length; i++){
                    residue[i]^=divisor[i];
                }
                residue = zeroElementsDeletion(residue);
            }
            return residue;
        }
        else {
            residue = zeroElementsDeletion(residue);
            return residue;
        }
    }

    public static Integer[] zeroElementsDeletion(Integer[] residue) {
        Integer[] newResidue;
        int i = 0;
        while (residue[i] == 0){
            Integer[] temp = new Integer[residue.length-1];
            System.arraycopy(residue,1,temp,0,residue.length-1);
            residue = temp;
            if(residue.length==0) {
                break;
            }
        }
        newResidue=residue;
        return newResidue;
    }

    public static Integer[] vectorSum(Integer[] vector1, Integer[] vector2){
        Integer[] result = vector1.clone();
        for(int i=0;i<vector2.length;i++){
            result[result.length-i-1] = vector1[vector1.length-i-1]^vector2[vector2.length-i-1];
        }
        return result;
    }

    public static byte[] coding(Integer[] infVector) {
        Integer[] polynomial = new Integer[7];
        Integer[] zeroMas = {0,0,0};

        System.arraycopy(infVector,0,polynomial,0,infVector.length);
        System.arraycopy(zeroMas,0,polynomial,infVector.length,zeroMas.length);

        Integer[] remainder = division(polynomial);

        Integer[] cycleCode = vectorSum(polynomial,remainder);
        int[] intCode = new int[7];
        for(int i = 0; i<cycleCode.length; i++) {
            intCode[i] = cycleCode[i];
        }
        
        ByteBuffer byteBuffer = ByteBuffer.allocate(intCode.length*4);        
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(intCode);

        byte[] array = byteBuffer.array();

        byte[] result = new byte[7];
        int count =3;
        for(int i=3,j=0; i<array.length; i+=4,j++) {
            result[j] = array[i];
        }
        return result;
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
    
    public static byte decoding(byte[] vector) {
        Integer[] firstResidue;
        Integer[] secondResidue;
        Integer[] firstPartInt = new Integer[7];
        Integer[] secondPartInt = new Integer[7];
        boolean error1;
        boolean error2;
        String byteStr;
        for(int i = 0; i < 14; i++) {
            if(i < 7) {
                firstPartInt[i] = (int)vector[i];
            } else {
                secondPartInt[i-7] = (int)vector[i];
            }
        }
        firstResidue = Encrypter.division(firstPartInt);
        secondResidue = Encrypter.division(secondPartInt);
        for(int m = 0; m < syndromeList.size(); m++) {
            //Сравниваем на наличие такого же вектора ошибки в списке векторов
            if ((error1 = Arrays.equals(firstResidue,syndromeList.get(m)))
                    || (error2 = Arrays.equals(secondResidue,syndromeList.get(m)))) {
                return -1;
            }
        }
        byteStr =  Encrypter.intToStr(firstPartInt, secondPartInt);
        return Byte.parseByte(byteStr,2);
    }
}

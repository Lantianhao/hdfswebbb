package Algorithm; /**
 * Created by xf123 on 12/16/17.
 */
/**
 * Created by xf123 on 12/12/17.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import FSOpt.FileOperation;
import Model.FileInfo;

public class LREVENODD {
    public FileInfo fileInfo;
    public int zeroNum;
    public int logicBlockSize;
    public static int pNum;
    private FileOperation fop;
    private int optFileSize;

    public LREVENODD(int p, FileInfo fileInfo, FileOperation ufop){
        pNum = p;
        this.fileInfo = fileInfo;

        fop = ufop;

        if(this.fileInfo.getFileSize()%(pNum*(pNum-1))==0){
            zeroNum = 0;
        }
        else{
            zeroNum = (int)(((this.fileInfo.getFileSize()/(pNum*(pNum-1)))+1)*(pNum*(pNum-1))-this.fileInfo.getFileSize());
        }
        logicBlockSize = (int)((this.fileInfo.getFileSize()+zeroNum)/(pNum*(pNum-1)));
    }

//    public LREVENODD(int p, String path, FileOperation ufop){
//        pNum = p;
//        filePath = path;
//        File f = new File(path);
//        fileName = f.getName();
//        fileSize = f.length();
//        System.out.println("Real up length "+fileSize);
//        fileType = fileName.substring(fileName.lastIndexOf("."), fileName.length());
//        fileName = fileName.substring(0, fileName.lastIndexOf("."));
//        fop = ufop;
//
//        if(fileSize%(pNum*(pNum-1))==0){
//            zeroNum = 0;
//        }
//        else{
//            zeroNum = (int)(((fileSize/(pNum*(pNum-1)))+1)*(pNum*(pNum-1))-fileSize);
//        }
//        logicBlockSize = (int)((fileSize+zeroNum)/(pNum*(pNum-1)));
//    }

    public void Encoding() throws Exception{
        // horizontal redundant and local redundant
        //reset
        setOptFileSize(0);
        for(int i=0; i<(pNum-1); i++){
            byte[] tempData = new byte[logicBlockSize];
            for(int j=0; j<(pNum); j++){
                byte[] bs = new byte[logicBlockSize];
                // should fixed
                RandomAccessFile r = new RandomAccessFile(this.fileInfo.getFilePath(), "r");
                r.seek((i*(pNum)+j)*logicBlockSize);
                if((i==pNum-2) && (j==pNum-1) && (zeroNum!=0)){
                    byte[] tempbs = new byte[logicBlockSize-zeroNum];
                    r.read(tempbs);
                    for(int l=0; l<logicBlockSize; l++){
                        if(l<(logicBlockSize-zeroNum)){
                            bs[l] = tempbs[l];
                        }
                        else{
                            bs[l] = 0;
                        }
                    }
                }
                else{
                    r.read(bs);
                }
                r.close();
                // save one data block to node
                WriteToFile(i, j, bs);
                // save local redundant while calculating horizontal redundant
                if(j==((pNum-1)/2)){
                    WriteToFile(i, pNum+2, tempData);
                }
                for(int k=0; k<logicBlockSize; k++){
                    tempData[k] = (byte)(tempData[k]^bs[k]);
                }
            }
            WriteToFile(i, pNum, tempData);
        }
        int sum = 0;
        // diagonal redundant
        byte[] sValue = new byte[logicBlockSize];
        for(int i=0; i<(pNum-1); i++){
            byte[] bs = new byte[logicBlockSize];
            // should fixed
            RandomAccessFile r = new RandomAccessFile(this.fileInfo.getFilePath(), "r");
            r.seek(((i*pNum)+(pNum-i-1))*logicBlockSize);
            r.read(bs);
            r.close();
            for(int k=0; k<logicBlockSize; k++){
                sValue[k] = (byte)(sValue[k]^bs[k]);
            }
        }
        for(int loop=0; loop<(pNum-1); loop++){
            byte[] tempData = new byte[logicBlockSize];
            for(int i=0; i<(pNum-1); i++){
                byte[] bs = new byte[logicBlockSize];
                int j = (loop-i+pNum)%pNum;
                // should fixed
                RandomAccessFile r = new RandomAccessFile(this.fileInfo.getFilePath(),"r");
                r.seek((i*pNum+j)*logicBlockSize);
                if(i==(pNum-2) && j==(pNum-1) && zeroNum!=0){
                    byte[] tempbs = new byte[logicBlockSize-zeroNum];
                    r.read(tempbs);
                    for(int l=0; l<logicBlockSize; l++){
                        if(l<(logicBlockSize-zeroNum)){
                            bs[l] = tempbs[l];
                        }
                        else{
                            bs[l] = 0;
                        }
                    }
                }
                else{
                    r.read(bs);
                }
                for(int k=0; k<logicBlockSize; k++){
                    tempData[k] = (byte)(tempData[k]^bs[k]);
                }
            }
            for(int k=0; k<logicBlockSize; k++){
                tempData[k] = (byte)(tempData[k]^sValue[k]);
            }
            WriteToFile(loop, pNum+1, tempData);
        }
    }

    public void Decoding(int[] errorPos) throws Exception{
        if (errorPos.length>3) {
            System.out.println("Too many errors to recover!");
        }
        else if(errorPos.length<=0 || (errorPos.length==2 && (errorPos[0]>=errorPos[1] || errorPos[0]>=errorPos[2] || errorPos[1]>=errorPos[2]))){
            System.out.println("Invalid errorPos");
        }
        else{
            // single disk failure
            if(errorPos.length == 1){
                System.out.println("Recover from single disk failure.");
                if(errorPos[0]<=0 || errorPos[0]>pNum+2){
                    System.out.println("Invalid errorPos");
                }
                // lost data disk in the front part
                else if(errorPos[0]<(pNum-1)/2){
                    System.out.println("Recover from data disk failure");
                    for(int i=0; i<(pNum-1); i++){
                        byte[] temp = new byte[logicBlockSize];
                        for(int j=0; j<(pNum-1)/2; j++){
                            if(j!=errorPos[0]){
                                byte[] nowBlock = ReadFromFile(i, j);
                                for(int k=0; k<logicBlockSize; k++){
                                    temp[k] = (byte)(temp[k]^nowBlock[k]);
                                }
                            }
                        }
                        // xor the local redundant
                        byte[] localRed = ReadFromFile(i,pNum+2);
                        for(int k=0; k<logicBlockSize; k++){
                            temp[k] = (byte)(temp[k]^localRed[k]);
                        }
                        WriteToFile(i, errorPos[0], temp);
                    }
                }
                // lost data disk in the later part
                else if(errorPos[0]>=(pNum-1)/2){
                    for(int i=0; i<(pNum-1); i++){
                        byte[] temp = new byte[logicBlockSize];
                        // xor the data disks in the later part and horizontal redundant
                        for(int j=(pNum-1)/2; j<pNum+1; j++){
                            if(j!=errorPos[0]){
                                byte[] nowBlock = ReadFromFile(i, j);
                                for(int k=0; k<logicBlockSize; k++){
                                    temp[k] = (byte)(temp[k]^nowBlock[k]);
                                }
                            }
                        }
                        // xor the local redundant
                        byte[] localRed = ReadFromFile(i,pNum+2);
                        for(int k=0; k<logicBlockSize; k++){
                            temp[k] = (byte)(temp[k]^localRed[k]);
                        }
                        WriteToFile(i, errorPos[0], temp);
                    }
                }
                else if(errorPos[0] == pNum){
                    System.out.println("Recover from horizontal redundant disk failure");
                    for(int i=0; i<(pNum-1); i++){
                        byte[] temp = new byte[logicBlockSize];
                        for(int j=0; j<pNum; j++){
                            byte[] nowBlock = ReadFromFile(i, j);
                            for(int k=0; k<logicBlockSize; k++){
                                temp[k] = (byte)(temp[k]^nowBlock[k]);
                            }
                        }
                        WriteToFile(i, pNum, temp);
                    }
                }
                else if(errorPos[0] == pNum+1){
                    System.out.println("Recover from diagonal redundant disk failure");
                    // calculate S
                    byte[] sValue = new byte[logicBlockSize];
                    for(int i=0; i<(pNum-1); i++){
                        byte[] bs = ReadFromFile(i, pNum-i-1);
                        for(int k=0; k<logicBlockSize; k++){
                            sValue[k] = (byte)(sValue[k]^bs[k]);
                        }
                    }
                    // calculate diagonal redundant disk
                    for(int loop=0; loop<(pNum-1); loop++){
                        byte[] tempData = new byte[logicBlockSize];
                        for(int i=0; i<(pNum-1); i++){
                            int j = (loop-i+pNum)%pNum;
                            byte[] bs = ReadFromFile(i, j);
                            for(int k=0; k<logicBlockSize; k++){
                                tempData[k] = (byte)(tempData[k]^bs[k]);
                            }
                        }
                        for(int k=0; k<logicBlockSize; k++){
                            tempData[k] = (byte)(tempData[k]^sValue[k]);
                        }
                        WriteToFile(loop, pNum+1, tempData);
                    }
                }
                // local redundant disk failure recover
                else{
                    System.out.println("Recover from local redundant disk failure");
                    for(int i=0; i<pNum; i++){
                        byte[] temp = new byte[logicBlockSize];
                        for(int j=0; j<(pNum-1)/2; j++){
                            byte[] nowBlock = ReadFromFile(i, j);
                            for(int k=0; k<logicBlockSize; k++){
                                temp[k] = (byte)(temp[k]^nowBlock[k]);
                            }
                        }
                        WriteToFile(i, pNum+2, temp);
                    }
                }
            }
            // double disk failures
            else if(errorPos.length == 2){
                // two data disks failed
                if(errorPos[0]<pNum && errorPos[1]<pNum){
                    System.out.println("Recover from two data disks failed.");
                    byte[] sValue = new byte[logicBlockSize];
                    // calculate S
                    for(int i=0; i<(pNum-1); i++){
                        byte[] temp1 = ReadFromFile(i, pNum);
                        byte[] temp2 = ReadFromFile(i, pNum+1);
                        for(int k=0; k<logicBlockSize; k++){
                            temp1[k] = (byte)(temp1[k]^temp2[k]);
                        }
                        for(int k=0; k<logicBlockSize; k++){
                            sValue[k] = (byte)(temp1[k]^sValue[k]);
                        }
                    }
                    int iniI = errorPos[1]-errorPos[0]-1;
                    int iniJ = errorPos[0];
                    int modNum = (iniI+iniJ)%(pNum-1);
                    for(int loop=0; loop<(pNum-1); loop++){
                        byte[] diaR = ReadFromFile(modNum, pNum+1);
                        byte[] reBlock_dia = new byte[logicBlockSize];
                        for(int k=0; k<logicBlockSize; k++){
                            reBlock_dia[k] = (byte)(sValue[k]^diaR[k]);
                        }
                        if(loop == 0){
                            iniI = (iniI+1)%(pNum-1);
                        }
                        for(int loop2=0; loop2<(pNum-1); loop2++){
                            if((modNum-((iniI+loop2)%(pNum-1))+pNum)%pNum==errorPos[0]){

                            }
                            else{
                                byte[] otherBlock = ReadFromFile((iniI+loop2)%(pNum-1), (modNum-((iniI+loop2)%(pNum-1))+pNum)%pNum);
                                for(int k=0; k<logicBlockSize; k++){
                                    reBlock_dia[k] = (byte)(reBlock_dia[k]^otherBlock[k]);
                                }
                            }
                        }
                        WriteToFile(modNum, errorPos[0], reBlock_dia);
                        byte[] reBlock_hor = new byte[logicBlockSize];
                        for(int j=0; j<pNum+1; j++){
                            if(j!=errorPos[1]){
                                byte[] otherBlock = ReadFromFile(modNum, j);
                                for(int k=0; k<logicBlockSize; k++){
                                    reBlock_hor[k] = (byte)(reBlock_hor[k]^otherBlock[k]);
                                }
                            }
                        }
                        WriteToFile(modNum, errorPos[1], reBlock_hor);
                        iniI = (iniI+(errorPos[1]-errorPos[0]))%(pNum-1);
                        modNum = (modNum+1)%(pNum-1);
                    }
                }
                // one data disk and horizontal redundant disk failed
                else if(errorPos[0]<pNum && errorPos[1]==pNum){
                    System.out.println("Recover from one data disk and horizontal redundant disk failed.");
                    // calculate S
                    byte[] sValue = new byte[logicBlockSize];
                    int iniI = 0;
                    int iniJ = (errorPos[0]-1+pNum)%pNum;
                    if(iniJ!=(pNum-1)){
                        byte[] diagRedundant = ReadFromFile(iniJ, pNum+1);
                        for(int k=0; k<logicBlockSize; k++){
                            sValue[k] = (byte)(sValue[k]^diagRedundant[k]);
                        }
                    }
                    for(int loop=0; loop<(pNum-1); loop++){
                        byte[] tempBlock = ReadFromFile(iniI, iniJ);
                        for(int k=0; k<logicBlockSize; k++){
                            sValue[k] = (byte)(sValue[k]^tempBlock[k]);
                        }
                        iniI = (iniI+1)%(pNum-1);
                        iniJ = (iniJ-1+pNum)%pNum;
                    }
                    for(int i=0; i<(pNum-1); i++){
                        byte[] reBlock_dia = new byte[logicBlockSize];
                        for(int k=0; k<logicBlockSize; k++){
                            reBlock_dia[k] = sValue[k];
                        }
                        iniI = (i+1)%(pNum-1);
                        iniJ = (pNum+errorPos[0]+i-iniI)%pNum;
                        if((iniI+iniJ)%(pNum-1)!=0 || iniJ==0){
                            byte[] dia_red = ReadFromFile((i+errorPos[0])%pNum, pNum+1);
                            for(int k=0; k<logicBlockSize; k++){
                                reBlock_dia[k] = (byte)(reBlock_dia[k]^dia_red[k]);
                            }
                        }
                        for(int loop=0; loop<(pNum-2); loop++){
                            byte[] otherBlock = ReadFromFile(iniI, iniJ);
                            for(int k=0; k<logicBlockSize; k++){
                                reBlock_dia[k] = (byte)(reBlock_dia[k]^otherBlock[k]);
                            }
                            iniI = (iniI+1)%(pNum-1);
                            iniJ = (pNum+errorPos[0]+i-iniI)%pNum;
                        }
                        WriteToFile(i, errorPos[0], reBlock_dia);
                        byte[] reBlock_hor=new byte[logicBlockSize];
                        for(int j=0; j<pNum; j++){
                            byte[] otherBlock = ReadFromFile(i, j);
                            for(int k=0; k<logicBlockSize; k++){
                                reBlock_hor[k] = (byte)(reBlock_hor[k]^otherBlock[k]);
                            }
                        }
                        WriteToFile(i, pNum, reBlock_hor);
                    }
                }
                // one data disk and diagonal redundant disk failed
                else if(errorPos[0]<pNum && errorPos[1]==(pNum+1)){
                    System.out.println("Recover from one data disk and diagonal redundant disk failure.");
                    // calculate data disk
                    for(int i=0; i<(pNum-1); i++){
                        byte[] reBlock_hor = new byte[logicBlockSize];
                        for(int j=0; j<(pNum+1); j++){
                            if(j!=errorPos[0]){
                                byte[] otherBlock = ReadFromFile(i, j);
                                for(int k=0; k<logicBlockSize; k++){
                                    reBlock_hor[k] = (byte)(reBlock_hor[k]^otherBlock[k]);
                                }
                            }
                        }
                        WriteToFile(i, errorPos[0], reBlock_hor);
                    }
                    // calculate S
                    byte[] sValue = new byte[logicBlockSize];
                    for(int i=0; i<(pNum-1); i++){
                        byte[] bs = ReadFromFile(i, pNum-i-1);
                        for(int k=0; k<logicBlockSize; k++){
                            sValue[k] = (byte)(sValue[k]^bs[k]);
                        }
                    }
                    // calculate redundant
                    for(int loop=0; loop<(pNum-1); loop++){
                        byte[] tempData = new byte[logicBlockSize];
                        for(int i=0; i<(pNum-1); i++){
                            int j = (loop-i+pNum)%pNum;
                            byte[] bs = ReadFromFile(i, j);
                            for(int k=0; k<logicBlockSize; k++){
                                tempData[k] = (byte)(tempData[k]^bs[k]);
                            }
                        }
                        for(int k=0; k<logicBlockSize; k++){
                            tempData[k] = (byte)(tempData[k]^sValue[k]);
                        }
                        WriteToFile(loop, pNum+1, tempData);
                    }
                }
                // one data disk and local redundant disk failed
                else if(errorPos[0]<pNum && errorPos[1] == pNum+2){
                    // use horizontal redundant to rebuild data disk
                    for(int i=0; i<(pNum-1); i++){
                        byte[] tempData = new byte[logicBlockSize];
                        for(int j=0; j<(pNum+1); j++){
                            if(j!=errorPos[0]){
                                byte[] otherBlock = ReadFromFile(i, j);
                                for(int k=0; k<logicBlockSize; k++){
                                    tempData[k] = (byte)(tempData[k]^otherBlock[k]);
                                }
                            }
                        }
                        WriteToFile(i, errorPos[0], tempData);
                    }
                    // then regenerate the local redundant disk
                    for(int i=0; i<(pNum-1); i++){
                        byte[] tempData = new byte[logicBlockSize];
                        for(int j=0; j<(pNum-1)/2; j++){
                            if(j!=errorPos[0]){
                                byte[] otherBlock = ReadFromFile(i,j);
                                for(int k=0; k<logicBlockSize; k++){
                                    tempData[k] = (byte)(tempData[k]^otherBlock[k]);
                                }
                            }
                        }
                        WriteToFile(i, errorPos[1], tempData);
                    }
                }
                // two redundant disks failed
                else{
                    //go to normal single disk failure recover
                    int[] newErrorPos1 = {errorPos[0]};
                    int[] newErrorPos2 = {errorPos[1]};
                    Decoding(newErrorPos1);
                    Decoding(newErrorPos2);
                }
            }
            // triple disk failure
            else{
                // three data disks failure
                if(errorPos[2] < pNum){
                    // unresolvable situation: three disks in same part
                    if(errorPos[0] >= (pNum-1)/2 || errorPos[2] < (pNum-1)/2){
                        System.out.println("Can not recover triple disk failure: three disks are in the same part");
                    }
                    //two disks in the front part
                    else if(errorPos[1] < (pNum-1)/2){
                        //use local redundant and horizontal redundant to recover the lost one data disk in the later
                        for(int i=0; i<(pNum-1); i++){
                            byte[] tempData = new byte[logicBlockSize];
                            // xor the later data disks and horizontal redundant
                            for(int j=(pNum-1)/2; j<(pNum+1); j++){
                                if(j!=errorPos[2]){
                                    byte[] bs = ReadFromFile(i, j);
                                    for(int k=0; k<logicBlockSize; k++){
                                        tempData[k] = (byte)(tempData[k]^bs[k]);
                                    }
                                }
                            }
                            // xor the local redundant
                            byte[] bs = ReadFromFile(i, pNum+2);
                            for(int k=0; k<logicBlockSize; k++){
                                tempData[k] = (byte)(tempData[k]^bs[k]);
                            }
                            WriteToFile(i, errorPos[2], tempData);
                        }
                        //then go to normal double disk failure recover
                        int[] newErrorPos = {errorPos[0], errorPos[1]};
                        Decoding(newErrorPos);
                    }
                    //two disks in the later part
                    else{
                        //use local redundant to recover the lost one data disk in the front
                        for(int i=0; i<(pNum-1); i++){
                            byte[] tempData = new byte[logicBlockSize];
                            // xor the front data disks
                            for(int j=0; j<(pNum-1)/2; j++){
                                if(j!=errorPos[0]){
                                    byte[] bs = ReadFromFile(i, j);
                                    for(int k=0; k<logicBlockSize; k++){
                                        tempData[k] = (byte)(tempData[k]^bs[k]);
                                    }
                                }
                            }
                            // xor the local redundant
                            byte[] bs = ReadFromFile(i, pNum+2);
                            for(int k=0; k<logicBlockSize; k++){
                                tempData[k] = (byte)(tempData[k]^bs[k]);
                            }
                            WriteToFile(i, errorPos[0], tempData);
                        }
                        //then go to normal double disk failure recover
                        int[] newErrorPos = {errorPos[1], errorPos[2]};
                        Decoding(newErrorPos);
                    }
                }
                // two data disk failure and one redundant disk failure
                else if(errorPos[1] < pNum){
                    //horizontal redundant disk failure
                    if(errorPos[2] == pNum){
                        if(errorPos[0]<(pNum-1)/2 && errorPos[1]>(pNum-1)/2){
                            //use local redundant to recover the lost one data disk in the front
                            for(int i=0; i<(pNum-1); i++){
                                byte[] tempData = new byte[logicBlockSize];
                                // xor the front data disks
                                for(int j=0; j<(pNum-1)/2; j++){
                                    if(j!=errorPos[0]){
                                        byte[] bs = ReadFromFile(i, j);
                                        for(int k=0; k<logicBlockSize; k++){
                                            tempData[k] = (byte)(tempData[k]^bs[k]);
                                        }
                                    }
                                }
                                // xor the local redundant
                                byte[] bs = ReadFromFile(i, pNum+2);
                                for(int k=0; k<logicBlockSize; k++){
                                    tempData[k] = (byte)(tempData[k]^bs[k]);
                                }
                                WriteToFile(i, errorPos[0], tempData);
                            }
                            //then go to normal double disk failure recover
                            int[] newErrorPos = {errorPos[1], errorPos[2]};
                            Decoding(newErrorPos);
                        }
                        else{
                            System.out.println("Can not recover triple disk failure: horizontal lost and two data disks in the same part.");
                        }
                    }
                    //diagonal redundant disk failure
                    else if(errorPos[2] == pNum+1){
                        if(errorPos[0]<(pNum-1)/2 && errorPos[1]>(pNum-1)/2){
                            //use local redundant to recover the lost one data disk in the front
                            for(int i=0; i<(pNum-1); i++){
                                byte[] tempData = new byte[logicBlockSize];
                                // xor the front data disks
                                for(int j=0; j<(pNum-1)/2; j++){
                                    if(j!=errorPos[0]){
                                        byte[] bs = ReadFromFile(i, j);
                                        for(int k=0; k<logicBlockSize; k++){
                                            tempData[k] = (byte)(tempData[k]^bs[k]);
                                        }
                                    }
                                }
                                // xor the local redundant
                                byte[] bs = ReadFromFile(i, pNum+2);
                                for(int k=0; k<logicBlockSize; k++){
                                    tempData[k] = (byte)(tempData[k]^bs[k]);
                                }
                                WriteToFile(i, errorPos[0], tempData);
                            }
                            //then go to normal double disk failure recover
                            int[] newErrorPos = {errorPos[1], errorPos[2]};
                            Decoding(newErrorPos);
                        }
                        else{
                            System.out.println("Can not recover triple disk failure: diagonal lost and two data disks in the same part");
                        }
                    }
                    //local redundant disk failure
                    else{
                        //go to normal double disk failure
                        int[] newErrorPos = {errorPos[0], errorPos[1]};
                        Decoding(newErrorPos);
                        //then regenerate local redundant disk
                        for(int i=0; i<(pNum-1); i++){
                            byte[] tempData = new byte[logicBlockSize];
                            for(int j=0; j<(pNum-1)/2; j++){
                                byte[] bs = ReadFromFile(i,j);
                                for(int k=0; k<logicBlockSize; k++){
                                    tempData[k] = (byte)(tempData[k]^bs[k]);
                                }
                            }
                            WriteToFile(i, pNum+2, tempData);
                        }
                    }
                }
                // one data disk failure and two redundant disk failure
                else if(errorPos[0] < pNum){
                    // horizontal and diagonal redundant disks failure
                    if(errorPos[1]==pNum && errorPos[2]==pNum+1){
                        // data disk is in the front part
                        if(errorPos[0]>(pNum-1)/2){
                            System.out.println("Can not recover from triple disk failure: Only local redundant disk can not recover data disk in the later part");
                        }
                        // data disk is in the later part
                        else{
                            //use local redundant to recover the lost one data disk in the front
                            for(int i=0; i<(pNum-1); i++){
                                byte[] tempData = new byte[logicBlockSize];
                                // xor the front data disks
                                for(int j=0; j<(pNum-1)/2; j++){
                                    if(j!=errorPos[0]){
                                        byte[] bs = ReadFromFile(i, j);
                                        for(int k=0; k<logicBlockSize; k++){
                                            tempData[k] = (byte)(tempData[k]^bs[k]);
                                        }
                                    }
                                }
                                // xor the local redundant
                                byte[] bs = ReadFromFile(i, pNum+2);
                                for(int k=0; k<logicBlockSize; k++){
                                    tempData[k] = (byte)(tempData[k]^bs[k]);
                                }
                                WriteToFile(i, errorPos[0], tempData);
                            }
                            //then go to normal double disks failure recover
                            int[] newErrorPos = {errorPos[1], errorPos[2]};
                            Decoding(newErrorPos);
                        }
                    }
                    // horizontal or diagonal redundant survive which means errorPos[2]=pNum+2
                    else{
                        int[] newErrorPos = {errorPos[0], errorPos[1]};
                        Decoding(newErrorPos);
                        //then regenerate local redundant disk
                        for(int i=0; i<(pNum-1); i++){
                            byte[] tempData = new byte[logicBlockSize];
                            for(int j=0; j<(pNum-1)/2; j++){
                                byte[] bs = ReadFromFile(i,j);
                                for(int k=0; k<logicBlockSize; k++){
                                    tempData[k] = (byte)(tempData[k]^bs[k]);
                                }
                            }
                            WriteToFile(i, pNum+2, tempData);
                        }
                    }
                }
                // three redundant failure
                else{
                    // go to normal double disk failure recover
                    int[] newErrorPos = {errorPos[0], errorPos[1]};
                    Decoding(newErrorPos);
                    // then regenerate local redundant disk
                    for(int i=0; i<(pNum-1); i++){
                        byte[] tempData = new byte[logicBlockSize];
                        for(int j=0; j<(pNum-1)/2; j++){
                            byte[] bs = ReadFromFile(i,j);
                            for(int k=0; k<logicBlockSize; k++){
                                tempData[k] = (byte)(tempData[k]^bs[k]);
                            }
                        }
                        WriteToFile(i, pNum+2, tempData);
                    }
                }
            }
        }
    }

    public void WriteToFile(int i, int j, byte[] fileContent) throws Exception{
        String targetPath = "/testdir/"+this.fileInfo.getUserName()+"_"+this.fileInfo.getFileName()+"_"+i+"_"+j+".txt";
        addOptFileSize(fileContent.length);
        fop.CreateAndWriteToFile(targetPath, fileContent);
    }

    public byte[] ReadFromFile(int i, int j) throws Exception{
        String targetPath = "/testdir/"+this.fileInfo.getUserName()+"_"+this.fileInfo.getFileName()+"_"+i+"_"+j+".txt";
        addOptFileSize(logicBlockSize);
        return fop.ReadFromFile(targetPath, logicBlockSize);
    }

    // rebuild source file to download
    public void RebuildFile(String targetPath) throws Exception{
        //reset
        setOptFileSize(0);
        System.out.println("Rebuild begin!");
        int[] errPos = GetErrorPos();
        if(errPos.length > 0){
            Decoding(errPos);
        }
        else {
            File resFile = new File(targetPath+"/"+this.fileInfo.getUserName()+"_"+this.fileInfo.getFileName()+this.fileInfo.getFileType());
            FileOutputStream out = new FileOutputStream(resFile);
            int sumSize = 0;
            for (int i = 0; i < (pNum - 1); i++) {
                for (int j = 0; j < (pNum); j++) {
                    byte[] tempFile = ReadFromFile(i, j);
                    byte[] realTempFile = null;
                    if (i == pNum - 2 && j == pNum - 1 && zeroNum != 0) {
                        realTempFile = new byte[logicBlockSize - zeroNum];
                        System.arraycopy(tempFile, 0, realTempFile, 0, logicBlockSize - zeroNum);
                    } else {
                        realTempFile = tempFile;
                    }
                    System.out.println("One get " + i + " " + j + " " + realTempFile.length);
                    sumSize = sumSize + realTempFile.length;
                    out.write(realTempFile);
                }
            }
            System.out.println("Real recv length " + sumSize);
            out.close();
        }
    }

    public int[] GetErrorPos() throws Exception{
        int[] errPos = new int[0];
        for(int j=0; j<pNum+2; j++){
            boolean colFlag = true;
            for(int i=0; i<pNum-1; i++){
                String path = "/testdir/"+this.fileInfo.getUserName()+"_"+this.fileInfo.getFileName()+"_"+i+"_"+j+".txt";
                boolean flag = fop.IsExisted(path);
                if (!flag && colFlag){
                    colFlag = false;
                    int[] tempErrPos = new int[errPos.length+1];
                    System.arraycopy(errPos, 0, tempErrPos, 0, errPos.length);
                    tempErrPos[tempErrPos.length-1] = j;
                    errPos = tempErrPos;
                }
            }
        }
        return errPos;
    }

    public void DeleteFile() throws Exception{
        //delete doesn't need file transition
        setOptFileSize(0);
        for (int i = 0; i < (pNum - 1); i++) {
            for (int j = 0; j < (pNum+3); j++) {
                String targetPath = "/testdir/"+this.fileInfo.getUserName()+"_"+this.fileInfo.getFileName()+"_"+i+"_"+j+".txt";
                fop.DeleteFile(targetPath);
            }
        }
    }

    public void setOptFileSize(int size){
        this.optFileSize = size;
    }

    public int getOptFileSize(){
        return this.optFileSize;
    }

    public void addOptFileSize(int size){
        this.optFileSize += size;
    }

    public boolean checkParam(){
        if((pNum+"").equals("")){
            return false;
        }
        else if(pNum<=0){
            return false;
        }
        else if(!isPrime(pNum)){
            return false;
        }
        else {
            return true;
        }
    }

    private static boolean isPrime(int p){
        boolean flag = true;
        for(int i=2;i<=p/2;i++){
            if(p%i==0){
                flag = false;
                break;
            }
        }
        return flag;
    }
}


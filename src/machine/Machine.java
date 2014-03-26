/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import static machine.Utils.*;

/**
 *
 * @author adomas
 */
public class Machine {

    public final static int WORD_SIZE = 4;
    public final static int BLOCK_SIZE = 10;
    public final static int BLOCKS = 70;
    public final static int USER_MEMORY_BLOCKS = 50;
    public final static int PAGE_TABLE_BLOCKS = 10;
    public final byte memory[] = new byte[WORD_SIZE * BLOCK_SIZE * BLOCKS];
    public final byte memoryBuffer[] = new byte[WORD_SIZE];
    public final byte PLR[] = new byte[WORD_SIZE];
    public final byte AX[] = new byte[WORD_SIZE];
    public final byte BX[] = new byte[WORD_SIZE];
    public final byte IC[] = new byte[2];
    public byte C;
    public byte MODE;
    public byte CH1;
    public byte CH2;
    public byte CH3;
    public byte IOI;
    public byte PI;
    public byte SI;
    public byte TI;
    public char X, Y;
    public int channelNumber;
    public Console console = System.console();
    public byte channelDeviceBuffer[] = new byte[40];
    public Scanner sc;

    /*
     * @param args the command line arguments
     */
    public int getNextAvaibleBlockIndex() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void readWord(int realAddress) {
        for (int i = 0; i < WORD_SIZE; i++) {
            memoryBuffer[i] = memory[realAddress * WORD_SIZE + i];
        }
    }

    public int realAddress(char x, char y) throws CastException {
        int block = byteToInt(PLR[2]) * 10 + byteToInt(PLR[3]);
        int a2 = memory[block + charToInt(x) * WORD_SIZE + 3];
        return (a2 * 10 + charToInt(y))*WORD_SIZE;
    }

    public void loader() throws CastException {

        int from = 4 * BLOCK_SIZE * WORD_SIZE;
        for (int i = 0; i < 40; i++) {
            memory[from + i] = intToByte(10 + i);
        }
        shuffle(memory, from, from + 40, 10);
        for (int i = 0; i < BLOCK_SIZE; i++) {
            memory[i * WORD_SIZE + 3] = memory[from + i];
        }
    }

    public void swap(byte[] memory, int from, int to) {
        byte temp = memory[from];
        memory[from] = memory[to];
        memory[to] = temp;
    }

    public void shuffle(byte[] memory, int from, int to, int size) {
        Random randomGenerator = new Random(System.currentTimeMillis());
        for (int i = 0; i < size; i++) {
            int random = randomGenerator.nextInt(to - from - i);
            System.out.println("random " + random);
            System.out.println("Swapping " + (from + i) + " " + (from + random + i));
            swap(memory, from + i, from + random + i);
        }
    }

    public static void main(String[] args) throws CastException, MachineException {
        // TODO code application logic here
        Machine machine = new Machine();
        /*String s = "LA90";
        char ch[] =  {s.charAt(0),s.charAt(1),s.charAt(2),s.charAt(3)};
        byte bytes[] = new byte [4];
        for(int i=0; i<4; i++) {
            bytes[i]=charToByte(ch[i]);
            System.out.println(byteToChar(bytes[i]));
        }*/
        //machine.loader();
        Loader ld = new Loader(machine);
        try {
            ld.loader("test.txt");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Machine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Machine.class.getName()).log(Level.SEVERE, null, ex);
        }
        machine.IC[0] = charToByte(intToChar(0));
        machine.IC[1] = charToByte(intToChar(0));
        //machine.printMemory();
        while (true) {
            //System.out.println("Channel "+machine.channelNumber);
            if (byteToInt(machine.CH1) + byteToInt(machine.CH2) + byteToInt(machine.CH3) == 0) {
                machine.printRegisters();
                //machine.printMemory();
                pause();
                try {
                    machine.intepreteNextCommand();
                } catch(MachineException e) {
                    e.printStackTrace();
                    break;
                }
            }
            machine.TI = intToByte(byteToInt(machine.TI) - 1);
            try {
                machine.StartIO();
            } catch (BufferOverflowException ex) {
                Logger.getLogger(Machine.class.getName()).log(Level.SEVERE, null, ex);
                break;
            } catch (IOException ex) {
                Logger.getLogger(Machine.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
            machine.checkInterrupt();
        }
    }

    public void resetC() {
        C = (byte) (C ^ C);
    }

    public void setSF(boolean val) {
        C = (byte) (val ? (C | 0b00000100) : (C ^ 0b00000100));
    }

    public void setOF(boolean val) {
        C = (byte) (val ? (C | 0b00000010) : (C ^ 0b00000010));
    }

    public void setZF(boolean val) {
        C = (byte) (val ? (C | 0b00000001) : (C ^ 0b00000001));
    }

    public boolean getSF() {
        return ((C & 0b00000100) > 0) ? true : false;
    }

    public boolean getOF() {
        return ((C & 0b00000100) > 0) ? true : false;
    }

    public boolean getZF() {
        return ((C & 0b00000100) > 0) ? true : false;
    }

    public void incIC() throws CastException {
        //System.out.println("IC");
        int x = charToInt(byteToChar(IC[0]));
        int y = charToInt(byteToChar(IC[1]));
        int IC_int = x * 10 + y;
        IC_int++;
        IC[0] = charToByte(intToChar(IC_int / 10));
        IC[1] = charToByte(intToChar(IC_int % 10));
    }

    public void commandLA(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            for (int i = 0; i < 4; i++) {
                AX[i] = memory[address + i];
            }
            int AX_int = wordToInt(AX, 0);
            if (AX_int == 0) {
                setZF(true);
            } else if (AX_int < 0) {
                setSF(true);
            }
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandLB(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            for (int i = 0; i < 4; i++) {
                BX[i] = memory[address + i];
            }
            int BX_int = wordToInt(BX, 0);
            if (BX_int == 0) {
                setZF(true);
            } else if (BX_int < 0) {
                setSF(true);
            }
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandLAfB() throws CastException {
        resetC();
        incIC();
        if (byteToInt(BX[0]) + byteToInt(BX[1]) > 0) {
            PI = intToByte(1);
            return;
        }
        try {
            int address = realAddress(byteToChar(BX[2]), byteToChar(BX[3]));
            for (int i = 0; i < 4; i++) {
                AX[i] = memory[address + i];
            }
            int AX_int = wordToInt(AX, 0);
            if (AX_int == 0) {
                setZF(true);
            } else if (AX_int < 0) {
                setSF(true);
            }
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandLBfA() throws CastException {
        resetC();
        incIC();
        if (AX[0] + AX[1] > 0) {
            PI = intToByte(1);
            return;
        }
        try {
            int address = realAddress(byteToChar(AX[2]), byteToChar(AX[3]));
            for (int i = 0; i < 4; i++) {
                BX[i] = memory[address + i];
            }
            int BX_int = wordToInt(AX, 0);
            if (BX_int == 0) {
                setZF(true);
            } else if (BX_int < 0) {
                setSF(true);
            }
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandSA(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            for (int i = 0; i < 4; i++) {
                memory[address + i] = AX[i];
            }
            int AX_int = wordToInt(AX, 0);
            if (AX_int == 0) {
                setZF(true);
            } else if (AX_int < 0) {
                setSF(true);
            }
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandSB(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            for (int i = 0; i < 4; i++) {
                memory[address + i] = BX[i];
            }
            int BX_int = wordToInt(BX, 0);
            if (BX_int == 0) {
                setZF(true);
            } else if (BX_int < 0) {
                setSF(true);
            }
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandCOPA() throws CastException {
        resetC();
        incIC();
        for (int i = 0; i < 4; i++) {
            AX[i] = BX[i];
        }
        int AX_int = wordToInt(AX, 0);
        if (AX_int == 0) {
            setZF(true);
        } else if (AX_int < 0) {
            setSF(true);
        }
    }

    public void commandCOPB() throws CastException {
        resetC();
        incIC();
        for (int i = 0; i < 4; i++) {
            BX[i] = AX[i];
        }
        int BX_int = wordToInt(AX, 0);
        if (BX_int == 0) {
            setZF(true);
        } else if (BX_int < 0) {
            setSF(true);
        }
    }

    public void commandAW(char x) throws CastException {
        resetC();
        incIC();
        for (int i = 0; i < 3; i++) {
            BX[i] = intToByte(0);
        }
        BX[3] = charToByte(x);
        int BX_int = wordToInt(AX, 0);
        if (BX_int == 0) {
            setZF(true);
        } else if (BX_int < 0) {
            setSF(true);
        }
    }

    public void commandAA(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            int AX_int = wordToInt(AX, 0), memory_int = wordToInt(memory, address);
            try {
                AX_int = addWithOverflow(AX_int, memory_int);
            } catch (OverflowException e) {
                setOF(true);
                PI = intToByte(4);
            }
            if (AX_int == 0) {
                setZF(true);
            } else if (AX_int < 0) {
                setSF(true);
            }
            intToWord(AX_int, AX, 0);
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandAB(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            int BX_int = wordToInt(BX, 0), memory_int = wordToInt(memory, address);
            try {
                BX_int = addWithOverflow(BX_int, memory_int);
            } catch (OverflowException e) {
                setOF(true);
                PI = intToByte(4);
            }
            if (BX_int == 0) {
                setZF(true);
            } else if (BX_int < 0) {
                setSF(true);
            }
            intToWord(BX_int, BX, 0);
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandBA(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            int AX_int = wordToInt(AX, 0), memory_int = wordToInt(memory, address);
            try {
                AX_int = subWithOverflow(AX_int, memory_int);
            } catch (OverflowException e) {
                setOF(true);
                PI = intToByte(4);
            }
            if (AX_int == 0) {
                setZF(true);
            } else if (AX_int < 0) {
                setSF(true);
            }
            intToWord(AX_int, AX, 0);
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandBB(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            int BX_int = wordToInt(BX, 0), memory_int = wordToInt(memory, address);
            try {
                BX_int = subWithOverflow(BX_int, memory_int);
            } catch (OverflowException e) {
                setOF(true);
                PI = intToByte(4);
            }
            if (BX_int == 0) {
                setZF(true);
            } else if (BX_int < 0) {
                setSF(true);
            }
            intToWord(BX_int, BX, 0);
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandMA(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            int AX_int = wordToInt(AX, 0), memory_int = wordToInt(memory, address);
            try {
                AX_int = mulWithOverflow(AX_int, memory_int);
            } catch (OverflowException e) {
                setOF(true);
                PI = intToByte(4);
            }
            if (AX_int == 0) {
                setZF(true);
            } else if (AX_int < 0) {
                setSF(true);
            }
            intToWord(AX_int, AX, 0);
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandMB(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            int BX_int = wordToInt(BX, 0), memory_int = wordToInt(memory, address);
            try {
                BX_int = mulWithOverflow(BX_int, memory_int);
            } catch (OverflowException e) {
                setOF(true);
                PI = intToByte(4);
            }
            if (BX_int == 0) {
                setZF(true);
            } else if (BX_int < 0) {
                setSF(true);
            }
            intToWord(BX_int, BX, 0);
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandDA(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            int AX_int = wordToInt(BX, 0), BX_int, memory_int = wordToInt(memory, address);
            if (memory_int == 0) {
                PI = intToByte(3);
                return;
            }
            BX_int = AX_int % memory_int;
            AX_int /= memory_int;
            if (AX_int == 0) {
                setZF(true);
            } else if (AX_int < 0) {
                setSF(true);
            }
            intToWord(AX_int, AX, 0);
            intToWord(BX_int, BX, 0);
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandDECA() throws CastException {
        resetC();
        incIC();
        int AX_int = wordToInt(AX, 0);
        try {
            AX_int = subWithOverflow(AX_int, 1);
        } catch (OverflowException e) {
            setOF(true);
        }
        if (AX_int == 0) {
            setZF(true);
        } else if (AX_int < 0) {
            setSF(true);
        }
        intToWord(AX_int, AX, 0);
    }

    public void commandDECB() throws CastException {
        resetC();
        incIC();
        int BX_int = wordToInt(BX, 0);
        try {
            BX_int = subWithOverflow(BX_int, 1);
        } catch (OverflowException e) {
            setOF(true);
        }
        if (BX_int == 0) {
            setZF(true);
        } else if (BX_int < 0) {
            setSF(true);
        }
        intToWord(BX_int, BX, 0);
    }

    public void commandINCA() throws CastException {
        resetC();
        incIC();
        int AX_int = wordToInt(AX, 0);
        try {
            AX_int = addWithOverflow(AX_int, 1);
        } catch (OverflowException e) {
            setOF(true);
        }
        if (AX_int == 0) {
            setZF(true);
        } else if (AX_int < 0) {
            setSF(true);
        }
        intToWord(AX_int, AX, 0);
    }

    public void commandINCB() throws CastException {
        resetC();
        incIC();
        int BX_int = wordToInt(BX, 0);
        try {
            BX_int = addWithOverflow(BX_int, 1);
        } catch (OverflowException e) {
            setOF(true);
        }
        if (BX_int == 0) {
            setZF(true);
        } else if (BX_int < 0) {
            setSF(true);
        }
        intToWord(BX_int, BX, 0);
    }

    public void commandCA(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            int AX_int = wordToInt(AX, 0), memory_int = wordToInt(memory, address);
            try {
                AX_int = subWithOverflow(AX_int, memory_int);
            } catch (OverflowException e) {
                setOF(true);
                PI = intToByte(4);
            }
            if (AX_int == 0) {
                setZF(true);
            } else if (AX_int < 0) {
                setSF(true);
            }
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandCB(char x, char y) throws CastException {
        resetC();
        incIC();
        try {
            int address = realAddress(x, y);
            int BX_int = wordToInt(BX, 0), memory_int = wordToInt(memory, address);
            try {
                BX_int = subWithOverflow(BX_int, memory_int);
            } catch (OverflowException e) {
                setOF(true);
                PI = intToByte(4);
            }
            if (BX_int == 0) {
                setZF(true);
            } else if (BX_int < 0) {
                setSF(true);
            }
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandIP(char x, char y) throws CastException {
        incIC();
        try {
            int address = realAddress(x, y);
            X = x;
            Y = y;
            SI = intToByte(1);
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandOP(char x, char y) throws CastException {
        incIC();
        try {
            int address = realAddress(x, y);
            X = x;
            Y = y;
            SI = intToByte(2);
        } catch (ClassCastException e) {
            PI = intToByte(1);
        }
    }

    public void commandJP(char x, char y) throws CastException {
        IC[0] = charToByte(x);
        IC[1] = charToByte(y);
    }

    public void commandJE(char x, char y) throws CastException {
        incIC();
        if (getZF()) {
            commandJP(x, y);
        }
    }

    public void commandJL(char x, char y) throws CastException {
        incIC();
        if (!getZF() && (getSF() == getOF())) {
            commandJP(x, y);
        }
    }

    public void commandJG(char x, char y) throws CastException {
        incIC();
        if (!getZF() && (getSF() != getOF())) {
            commandJP(x, y);
        }
    }

    public void commandHALT() throws CastException {
        incIC();
        SI = intToByte(3);
    }

    public void commandGEC(char x) throws CastException {
        incIC();
        if (x < '1' || '3' < x) {
            PI = intToByte(1);
            return;
        }
        intToWord(0, BX, 0);
        if (x == '1' && getSF()) {
            BX[3] = intToByte(1);
        }
        if (x == '2' && getOF()) {
            BX[3] = intToByte(1);
        }
        if (x == '3' && getZF()) {
            BX[3] = intToByte(1);
        }
    }

    public void commandSEC(char x) throws CastException {
        incIC();
        if (x < '1' || '3' < x) {
            PI = intToByte(1);
            return;
        }
        boolean val = false;
        int BX_int = wordToInt(BX, 0);
        if (BX_int == 1) {
            val = true;
        }
        if (x == '1') {
            setSF(val);
        }
        if (x == '2' && getOF()) {
            setOF(val);
        }
        if (x == '3') {
            setZF(val);
        }
    }

    public void commandGEIC() throws CastException {
        intToWord(0, BX, 0);
        BX[2] = IC[0];
        BX[3] = IC[1];
        incIC();
    }

    public void commandSEIC() {
        IC[0] = BX[2];
        IC[1] = BX[3];
    }

    public void StartIO() throws CastException, BufferOverflowException, IOException {
        if (channelNumber == 1) {
            channelNumber = 0;
            CH1 = intToByte(1);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String input = br.readLine();
            int len = input.length();
            if (len > 40) {
                len = 40;
            }
            for (int i = 0; i < len; i++) {
                try {
                    channelDeviceBuffer[i] = charToByte(input.charAt(i));
                } catch (ClassCastException e) {
                    channelDeviceBuffer[i] = charToByte('?');
                }
            }
            if (len < 40) {
                channelDeviceBuffer[len] = charToByte('#');
            }
            int startPoz = charToInt(X) * 10 + charToInt(Y);
            outerloopCH1:
            for (int i = 0; i < BLOCK_SIZE; i++) {
                try {
                    char x = intToChar((startPoz + i) / 10);
                    char y = intToChar((startPoz + i) % 10);
                    int address = realAddress(x, y);
                    for (int j = 0; j < 4; j++) {
                        if (channelDeviceBuffer[i * WORD_SIZE + j] == '#') {
                            break outerloopCH1;
                        }
                        memory[address + j] = channelDeviceBuffer[i * WORD_SIZE + j];
                    }
                } catch (CastException e) {
                    throw new BufferOverflowException("Do not write a poem.");
                }
            }
            CH1 = intToByte(0);
            IOI = intToByte(byteToInt(IOI) + 1);
        }
        if (channelNumber == 2) {
            channelNumber = 0;
            CH2 = intToByte(1);
            int startPoz = charToInt(X) * 10 + charToInt(Y);
            outerloopCH2:
            for (int i = 0; i < 10; i++) {
                try {
                    char x = intToChar((startPoz + i) / 10);
                    char y = intToChar((startPoz + i) % 10);
                    int address = realAddress(x, y);
                    for (int j = 0; j < 4; j++) {
                        channelDeviceBuffer[i * WORD_SIZE + j] = memory[address + j];
                        if (memory[address + j] == '#') {
                            break outerloopCH2;
                        }
                    }
                } catch (CastException e) {
                    throw new BufferOverflowException("Do not write a poem.");
                }
            }
            for(int i=0;i<40;i++) {
                if(channelDeviceBuffer[i]==charToByte('#')) {
                    System.out.println();
                    break;
                }
                System.out.write(byteToInt(channelDeviceBuffer[i]));
            }
            CH2 = intToByte(0);
            IOI = intToByte(byteToInt(IOI) + 2);
        }
        if (channelNumber == 3) {
            channelNumber = 0;
            CH3 = intToByte(1);
            CH3 = intToByte(0);
            IOI = intToByte(byteToInt(IOI) + 4);
        }
    }

    public void intepreteNextCommand() throws CastException, MachineException {
        try {
            char x = byteToChar(IC[0]);
            char y = byteToChar(IC[1]);
            int address = realAddress(x, y);
            String command = "";
            for (int i = 0; i < 4; i++) {
                command += byteToChar(memory[address + i]);
            }
            //System.out.println("\t"+command);
            String commandStart = command.substring(0, 2);
            x = command.charAt(2);
            y = command.charAt(3);
            switch (commandStart) {
                case "LA":
                    if (x == 'f' && y == 'B') {
                        commandLAfB();
                    } else {
                        commandLA(x, y);
                    }
                    break;
                case "LB":
                    if (x == 'f' && y == 'A') {
                        commandLBfA();
                    } else {
                        commandLB(x, y);
                    }
                    break;
                case "SA":
                    commandSA(x, y);
                    break;
                case "SB":
                    commandSB(x, y);
                    break;
                case "CO":
                    if (x == 'P' && y == 'A') {
                        commandCOPA();
                    } else if (x == 'P' && y == 'B') {
                        commandCOPB();
                    } else {
                        throw new MachineException(command);
                    }
                    break;
                case "AW":
                    commandAW(x);
                    break;
                case "AA":
                    commandAA(x, y);
                    break;
                case "AB":
                    commandAB(x, y);
                    break;
                case "BA":
                    commandBA(x, y);
                    break;
                case "BB":
                    commandBB(x, y);
                    break;
                case "MA":
                    commandMA(x, y);
                    break;
                case "MB":
                    commandMB(x, y);
                    break;
                case "DA":
                    commandDA(x, y);
                    break;
                case "DE":
                    if (x == 'C' && y == 'A') {
                        commandDECA();
                    } else if (x == 'C' && y == 'B') {
                        commandDECB();
                    } else {
                        throw new MachineException(command);
                    }
                    break;
                case "IN":
                    if (x == 'C' && y == 'A') {
                        commandINCA();
                    } else if (x == 'C' && y == 'B') {
                        commandINCB();
                    } else {
                        throw new MachineException(command);
                    }
                    break;
                case "CA":
                    commandCA(x, y);
                    break;
                case "CB":
                    commandCB(x, y);
                    break;
                case "IP":
                    commandIP(x, y);
                    break;
                case "OP":
                    commandOP(x, y);
                    break;
                case "JP":
                    commandJP(x, y);
                    break;
                case "JE":
                    commandJE(x, y);
                    break;
                case "JL":
                    commandJL(x, y);
                    break;
                case "JG":
                    commandJG(x, y);
                    break;
                case "HA":
                    if (x == 'L' && y == 'T') {
                        commandHALT();
                    } else {
                        throw new MachineException(command);
                    }
                    break;
                case "GE":
                    if (x == 'C') {
                        commandGEC(y);
                    } else if (x == 'I' && y == 'C') {
                        commandGEIC();
                    } else {
                        throw new MachineException(command);
                    }
                    break;
                case "SE":
                    if (x == 'C') {
                        commandSEC(y);
                    } else if (x == 'I' && y == 'C') {
                        commandSEIC();
                    } else {
                        throw new MachineException(command);
                    }
                    break;
                default:
                    incIC();
                    throw new MachineException(command);
            }
        } catch (MachineException e) {
            PI = intToByte(1);
            throw e;
        }
    }

    public void checkInterrupt() throws CastException {

        if (byteToInt(TI) == 0) {
            System.out.println("Program has exceeded its time limit");
            MODE = 1;
            restartTimer();
            MODE = 0;
        }

        if (byteToInt(PI) != 0) {
            switch (byteToInt(PI)) {
                case 1:
                    System.out.println("PROGRAM INTERRUPT! Incorrect command");
                    MODE = 1;
                    stopProgram();
                    break;
                case 2:
                    System.out.println("PROGRAM INTERRUPT! Negative result");
                    MODE = 1;
                    stopProgram();
                    break;
                case 3:
                    System.out.println("PROGRAM INTERRUPT! Division by zero");
                    MODE = 1;
                    stopProgram();
                    break;
                case 4:
                    System.out.println("PROGRAM INTERRUPT! Program overflow!");
                    MODE = 1;
                    stopProgram();
                    break;
            }
        }

        if (byteToInt(SI) != 0) {
            switch (byteToInt(SI)) {
                case 1:
                    System.out.println("PROGRAM INTERRUPT! Data input!");
                    MODE = 1;
                    channelNumber = 1;
                    MODE = 0;
                    SI = 0;
                    break;
                case 2:
                    System.out.println("PROGRAM INTERRUPT! Data output!");
                    MODE = 1;
                    channelNumber = 2;
                    MODE = 0;
                    SI = 0;
                    break;
                case 3:
                    System.out.println("PROGRAM INTERRUPT! Command halt!");
                    MODE = 1;
                    stopProgram();
                    break;
            }
        }
        if (byteToInt(IOI) != 0) {
            switch (byteToInt(IOI)) {
                case 1:
                    System.out.println("Channel 1 done");
                    MODE = 1;
                    IOI= 0;
                    MODE = 0;
                    break;
                case 2:
                    System.out.println("Channel 2 done");
                    MODE = 1;
                    IOI= 0;
                    MODE = 0;
                    break;
                case 4:
                    System.out.println("Channel 3 done");
                    MODE = 1;
                    IOI= 0;
                    MODE = 0;
                    break;
            }
        }
    }

    void restartTimer() throws CastException {

        if (byteToInt(MODE) == 1) {
            TI = intToByte(100);
            System.out.println("Supervisor=> Timer restarted successfully. ");
        }
    }

    public void stopProgram() {
        System.out.println("ate");
        System.exit(0);
    }
 
    public void printMemory() throws CastException {
        
        System.out.println("Memory");
        for(int i=0; i<100; i++) {
            System.out.println("i "+i);
            char x = intToChar(i/10);
            char y = intToChar(i%10);
            int address = realAddress(x, y);
            for(int j=0; j<4; j++) {
                System.out.print(byteToChar(memory[address+j]));
            }
            System.out.println();
        }
    }
    public void printRegisters() {
        System.out.print("AX = ");
        for(int i=0; i<4; i++) {
            System.out.print("|"+byteToInt(AX[i]));
        }
        System.out.println();
        System.out.print("BX = ");
        for(int i=0; i<4; i++) {
            System.out.print("|"+byteToInt(BX[i]));
        }
        System.out.println();
        System.out.print("IC = ");
        for(int i=0; i<2; i++) {
            System.out.print("|"+byteToInt(IC[i]));
        }
        System.out.println();
        System.out.println("C = "+byteToInt(C));
        System.out.println("CH1 = "+byteToInt(CH1));
        System.out.println("CH2 = "+byteToInt(CH2));
        System.out.println("CH3 = "+byteToInt(CH3));
        System.out.println("IOI = "+byteToInt(IOI));
        System.out.println("PI = "+byteToInt(PI));
        System.out.println("SI = "+byteToInt(SI));
        System.out.println("TI = "+byteToInt(TI));
    }
    public static void pause(){

          System.out.println("Press Any Key To Continue...");
          new java.util.Scanner(System.in).nextLine();
     }
}

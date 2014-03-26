/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import static machine.Utils.*;
import static machine.Machine.BLOCK_SIZE;
import static machine.Machine.WORD_SIZE;

public class Loader {

    byte[] memory;
    Machine machine;

    public Loader(Machine machine) {
        memory = machine.memory;
        this.machine = machine;
    }

    public void loader(String filename) throws FileNotFoundException, MachineException, IOException {
        if (filename == null) {
            filename = "";
        }
        File file = new File(filename);
        if (!file.canRead()) {
            throw new FileNotFoundException("Cannot read the file, doh.");
        }
        int from = 4 * BLOCK_SIZE * WORD_SIZE;
        //System.out.println("From " + from);
        for (int i = 0; i < 40; i++) {
            memory[from + i] = intToByte(10 + i);
        }
        shuffle(memory, from, from + 40, 10);
        for (int i = 0; i < BLOCK_SIZE; i++) {
            memory[i * WORD_SIZE + 3] = memory[from + i];
        }
        
        /*skaitymas*/
        BufferedReader inputStream = null;
        try {
            //System.out.println("nu");
            inputStream = new BufferedReader(new FileReader(filename));
            String l;
            byte dat[] = new byte[2];
            byte code[] = new byte[2];
            code[0]= intToByte(48);
            code[1]= intToByte(48);
            l = inputStream.readLine();
            if (!l.startsWith("$WOW")) {
                throw new MachineException("Invalid program label " + l);
            }
            l = inputStream.readLine();
            if (!l.startsWith(".NAM")) {
                throw new MachineException("Invalid program label " + l);
            }
            l = inputStream.readLine();
            if ((!l.startsWith(".DAT")) && (!l.startsWith("$WRT"))) {
                throw new MachineException("Invalid program laber" + l);
            }
            if (l.startsWith(".DAT")) {
                if (l.length() > 6) {
                    throw new IllegalArgumentException("Invalid amount of memory required ");
                }
                dat = segmentAdr(l);
                l = inputStream.readLine();
                if (!l.startsWith("$DAT")) {
                    throw new MachineException("Invalid program label " + l);
                }
                while (!l.startsWith("$WRT") || (!l.startsWith("$END"))) {
                    
                    l = inputStream.readLine();
                    if(l.startsWith("$WRT") || (l.startsWith("$END"))){
                        break;
                    }
                    l = checkLength(l);
                    writeToMem(l, dat);
                    dat = nextAdr(dat);
                }
            }

            while (!l.startsWith("$END")) {
                l = inputStream.readLine();
                if (l.startsWith("$END")){
                    break;
                }
                l = checkLength(l);
                writeToMem(l, code);
                code = nextAdr(code);
                
                }

            machine.C = intToByte(0);
                    
        } catch (IOException ex) {
            System.err.println(ex);
        }
        catch(MachineException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

    }

    public byte[] segmentAdr(String s) throws CastException {
        byte by[] = new byte[2];
        char ch = s.charAt(5);
        int a = (BLOCK_SIZE - charToInt(ch) );
        by[0] = intToByte(a+48);
        by[1] = charToByte('0');
        return by;
    }

    public String checkLength(String s) {
        try {
            machine.C = intToByte(0);
        } catch (CastException ex) {
            Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (s.startsWith("\"") && (s.endsWith("\""))) {
            try {
                machine.C = intToByte(1);
                s = s.substring(1,(s.length() - 1)); 
                if (s.length() < 4) {
                while (s.length() < 4) {
                    s = s + " ";
                }
            }
            } catch (CastException ex) {
                Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (Character.isDigit(s.charAt(0))) {
            try {
                machine.C = intToByte(2);
            } catch (CastException ex) {
                Logger.getLogger(Loader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (machine.C == 0) {
            if (s.length() > 4) {
                s = s.substring(0, 4);
            }
            if (s.length() < 4) {
                while (s.length() < 4) {
                    s = s + " ";
                }
            }
        }
                        //System.out.println("irasomas stringas : "+s);
        return s;
    }

    public void writeToMem(String s, byte[] x) throws CastException {
        char ch[];
        int b = 0;
        int address = machine.realAddress(Utils.byteToChar(x[0]), Utils.byteToChar(x[1]));
        if (byteToInt(machine.C) == 1) {
            ch = s.toCharArray();
            //System.out.print("pries irasyma i atminti: ");
            //System.out.print(ch[0]);
                //System.out.print(ch[1]);
                //System.out.print(ch[2]);
                //System.out.print(ch[3]);
                //System.out.println();
            for (int i = 0; i < 4; i++) {
                memory[address + i] = (Utils.charToByte(s.charAt(i)));
            }
        } else if (byteToInt(machine.C) == 2) {
            ch = s.toCharArray();
//            System.out.print("pries irasyma i atminti: ");
//            System.out.print(ch[0]);
//                System.out.print(ch[1]);
//                System.out.print(ch[2]);
//                System.out.print(ch[3]);
//                System.out.println();
            for (int i = 0; i < 4; i++) {
                memory[address + i] = (Utils.charToByte(s.charAt(i)));
            }
//            b = Integer.parseInt(s);
//            System.out.println("pries irasyma i atminti: "+b);
//            intToWord(b, machine.memory, address);
        } else if (byteToInt(machine.C) == 0) {
            ch = s.toCharArray();
//            System.out.print("pries irasyma i atminti: ");
//            System.out.print(ch[0]);
//                System.out.print(ch[1]);
//                System.out.print(ch[2]);
//                System.out.print(ch[3]);
//            System.out.println();
            for (int i = 0; i < 4; i++) {
                memory[address + i] = (Utils.charToByte(s.charAt(i)));
            }
        }

        //System.out.print("atmintyje: ");
        for (int i=0; i<4;i++){
    //System.out.print(byteToChar(machine.memory[address+i])+" ");
}
    //System.out.println();
    //System.out.println(address);
    }

    public byte[] nextAdr( byte x[]) throws CastException {
        int a = byteToInt(x[0]);
        int b = byteToInt(x[1]);
        b += 1;
        if (b > 57) {
            a += 1;
            b = 48;
        }
        if (a > 57) {
            throw new IllegalArgumentException("Virtual machine has no more space,"+a+","+b);
        }
        x[0] = intToByte(a);
        x[1] = intToByte(b);
        return x;
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
            //System.out.println("random " + random);
            //System.out.println("Swapping " + (from + i) + " " + (from + random + i));
            swap(memory, from + i, from + random + i);
        }
    }
}

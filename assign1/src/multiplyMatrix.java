import java.util.Arrays;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class multiplyMatrix {

    public static void onMult(int m_ar, int m_br) {
        long time1, time2;
        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_br * m_br];
        double[] phc = new double[m_ar * m_br];

        // Initialize matrices
        Arrays.fill(pha, 1.0);
        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i * m_br + j] = i + 1;
            }
        }

        // Matrix multiplication
        time1 = System.currentTimeMillis();
        double temp;
        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_br; j++) {
                temp = 0;
                for (int k = 0; k < m_ar; k++) {
                    temp += pha[i * m_ar + k] * phb[k * m_br + j];
                }
                phc[i * m_ar + j] = temp;
            }
        }
        time2 = System.currentTimeMillis();
        System.out.printf("Time: %.3f seconds\n", (time2 - time1) / 1000.0);

        // Display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix: ");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_br); j++)
                System.out.print(phc[j] + " ");
        }
        System.out.println();
    }

    public static void onMultLine(int m_ar, int m_br) {

        long time1, time2;
        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_br * m_br];
        double[] phc = new double[m_ar * m_br];
    
        // Initialize matrices
        for (int i = 0; i < pha.length; i++) {
            pha[i] = 1.0;
        }
        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i * m_br + j] = i + 1;
            }
        }
        for (int i = 0; i < phc.length; i++) {
            phc[i] = 0.0; // Explicitly initializing phc to 0.0 for clarity
        }
    
        // Matrix multiplication
        time1 = System.nanoTime();
        for (int i = 0; i < m_ar; i++) {
            for (int k = 0; k < m_ar; k++) {
                for (int j = 0; j < m_br; j++) {
                    phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
                }
            }
        }
        time2 = System.nanoTime();
        System.out.printf("Time: %.3f seconds\n", (time2 - time1) / 1_000_000_000.0);
    
        // Display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix:");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_br); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println("\n");
    }
    
    public static void onMultBlock(int m_ar, int m_br, int bkSize) {

        long time1, time2;
        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_br * m_br];
        double[] phc = new double[m_ar * m_br];
    
        // Initialize matrices
        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_ar; j++) {
                pha[i * m_ar + j] = 1.0;
            }
        }
        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i * m_br + j] = i + 1;
            }
        }
    
        // Block matrix multiplication
        time1 = System.nanoTime();
        for (int i = 0; i < m_ar; i += bkSize) {
            for (int j = 0; j < m_br; j += bkSize) {
                for (int k = 0; k < m_ar; k += bkSize) {
                    for (int ii = i; ii < Math.min(i + bkSize, m_ar); ii++) {
                        for (int jj = j; jj < Math.min(j + bkSize, m_br); jj++) {
                            for (int kk = k; kk < Math.min(k + bkSize, m_ar); kk++) {
                                phc[ii * m_ar + jj] += pha[ii * m_ar + kk] * phb[kk * m_br + jj];
                            }
                        }
                    }
                }
            }
        }
        time2 = System.nanoTime();
        System.out.printf("Time: %.3f seconds\n", (time2 - time1) / 1_000_000_000.0);
    
        // Display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix:");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_br); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();
    }
    
    public static void main(String[] args) {
        PrintStream fileOut = null;
        try {
            fileOut = new PrintStream("./outputJava.txt");
            System.setOut(fileOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return; // Stop execution if file not found
        }

        // Executing Standard Multiplication
        System.out.println("---------------- Executing Standard Multiplication ----------------");
        for (int size = 600; size <= 3000; size += 400) {
            System.out.println();
            System.out.println("Running onMult with size " + size);
            System.out.println();
            onMult(size, size);
        }

        // Executing Line Multiplication
        System.out.println("---------------- Executing Line Multiplication ----------------");
        for (int size = 600; size <= 3000; size += 400) {
            System.out.println();
            System.out.println("Running onMultLine with size " + size);
            System.out.println();
            onMultLine(size, size);
        }
        for (int size = 4096; size <= 10240; size += 2048) {
            System.out.println();
            System.out.println("Running onMultLine with size " + size);
            System.out.println();
            onMultLine(size, size);
        }

        // Executing Block Multiplication
        System.out.println("---------------- Executing Block Multiplication ----------------");
        int[] blockSizes = {128, 256, 512};
        for (int size = 4096; size <= 10240; size += 2048) {
            for (int bkSize : blockSizes) {
                System.out.println();
                System.out.println("Running onMultBlock with size " + size + " and block size " + bkSize);
                System.out.println();
                onMultBlock(size, size, bkSize);
            }
        }

        if (fileOut != null) {
            fileOut.close();
        }
    }

}

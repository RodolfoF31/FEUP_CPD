import sys
import time

def on_mult(m_ar, m_br):
    
    pha = [[1.0 for _ in range(m_ar)] for _ in range(m_ar)]
    phb = [[j+1 for j in range(m_br)] for _ in range(m_br)]
    phc = [[0.0 for _ in range(m_br)] for _ in range(m_ar)]

    time1 = time.time()
    for i in range(m_ar):
        for j in range(m_br):
            temp = 0
            for k in range(m_ar):
                temp += pha[i][k] * phb[k][j]  # Correct indexing for pure Python lists
            phc[i][j] = temp
    time2 = time.time()

    print(f"Time: {time2 - time1:.3f} seconds")
    print("Result matrix:")
    for i in range(min(1, m_ar)):
        for j in range(min(10, m_br)):
            print(phc[i][j], end=" ")
    print("\n")
      
def on_mult_line(m_ar, m_br):
    
    pha = [[1.0 for _ in range(m_ar)] for _ in range(m_ar)]
    phb = [[j+1 for j in range(m_br)] for _ in range(m_br)]
    phc = [[0.0 for _ in range(m_br)] for _ in range(m_ar)]

    time1 = time.time()

    for i in range(m_ar):
        for k in range(m_ar):
            for j in range(m_br):
                phc[i][j] += pha[i][k] * phb[k][j]
                
    time2 = time.time()

    print(f"Time: {time2 - time1:.3f} seconds")
    print("Result matrix:")
    for i in range(min(1, m_ar)):
        for j in range(min(10, m_br)):
            print(phc[i][j], end=" ")
    print("\n")
    
    
def on_mult_block(m_ar, m_br, bk_size):
    
    pha = [[1.0 for _ in range(m_ar)] for _ in range(m_ar)]
    phb = [[j+1 for j in range(m_br)] for _ in range(m_br)]
    phc = [[0.0 for _ in range(m_br)] for _ in range(m_ar)]

    time1 = time.time()

    # Block matrix multiplication
    for i in range(0, m_ar, bk_size):
        for j in range(0, m_br, bk_size):
            for k in range(0, m_ar, bk_size):
                for ii in range(i, min(i + bk_size, m_ar)):
                    for jj in range(j, min(j + bk_size, m_br)):
                        for kk in range(k, min(k + bk_size, m_ar)):
                            phc[ii][jj] += pha[ii][kk] * phb[kk][jj]

    time2 = time.time()
    print(f"Time: {time2 - time1:.3f} seconds\n")

    # Display elements of the result matrix to verify correctness
    print("Result matrix:")
    for i in range(1):  # This iterates only once; for the first row
        for j in range(min(10, m_br)):
            print(phc[i][j], end=" ")  # Corrected indexing for accessing list elements
        print("\n")

def main():
    original_stdout = sys.stdout  # Save the original standard output

    with open('outputPY.txt', 'w') as f:
        sys.stdout = f  # Set the standard output to the file we created.

        # Executing Standard Multiplication
        print("---------------- Executing Standard Multiplication ----------------")
        for size in range(600, 3001, 400):
            print(f"\nRunning on_mult with size {size}\n")
            on_mult(size, size)

        # Executing Line Multiplication
        print("---------------- Executing Line Multiplication ----------------")
        for size in range(600, 3001, 400):
            print(f"\nRunning on_mult_line with size {size}\n")
            on_mult_line(size, size)

        for size in range(4096, 10241, 2048):
            print(f"\nRunning on_mult_line with size {size}\n")
            on_mult_line(size, size)

        # Executing Block Multiplication
        print("---------------- Executing Block Multiplication ----------------")
        block_sizes = [128, 256, 512]
        for size in range(4096, 10241, 2048):
            for bk_size in block_sizes:
                print(f"\nRunning on_mult_block with size {size} and block size {bk_size}\n")
                on_mult_block(size, size, bk_size)

    sys.stdout = original_stdout  # Restore the standard output to its original state

if __name__ == "__main__":
    main()
#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <chrono>
#include <cstdlib>
#include <fstream>

using namespace std;
using namespace std::chrono;

#define SYSTEMTIME high_resolution_clock::time_point

// add code here for line x line matriz multiplication
void OnMultLineOuterLoop(int m_ar, int m_br)
{
	SYSTEMTIME Time1, Time2;

	char st[100];
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (int i = 0; i < m_br; i++)
	{
		for (int j = 0; j < m_br; j++)
		{
			phb[i * m_br + j] = (double)(i + 1);
			phc[i * m_br + j] = 0.0;
		}
	}

	Time1 = high_resolution_clock::now();

	#pragma omp parallel for
	for (i = 0; i < m_ar; i++)
	{
		for (k = 0; k < m_ar; k++)
		{
			for (j = 0; j < m_br; j++)
			{
				phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
			}
		}
	}

	Time2 = high_resolution_clock::now();
	sprintf(st, "Time: %3.3f seconds\n", duration_cast<duration<double>>(Time2 - Time1).count());
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	free(pha);
	free(phb);
	free(phc);
}

void OnMultLineInnerLoop(int m_ar, int m_br)
{
	SYSTEMTIME Time1, Time2;

	char st[100];
	int i, j, k;

	double *pha, *phb, *phc;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (int i = 0; i < m_br; i++)
	{
		for (int j = 0; j < m_br; j++)
		{
			phb[i * m_br + j] = (double)(i + 1);
			phc[i * m_br + j] = 0.0;
		}
	}

	Time1 = high_resolution_clock::now();
	#pragma omp parallel
	for (i = 0; i < m_ar; i++)
	{
		for (k = 0; k < m_ar; k++)
		{
			#pragma omp for
			for (j = 0; j < m_br; j++)
			{
				phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
			}
		}
	}

	Time2 = high_resolution_clock::now();
	sprintf(st, "Time: %3.3f seconds\n", duration_cast<duration<double>>(Time2 - Time1).count());
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	free(pha);
	free(phb);
	free(phc);
}

int main(int argc, char *argv[])
{

	freopen("outputCppParallel.txt", "w", stdout);

	// Execute OnMultLine for matrix sizes 600x600 to 3000x3000 with increments of 400
	// And then for sizes 4096x4096 to 10240x10240 with intervals of 2048
	cout << "---------------- Executing Line Multiplication Parallel OuterLoop ----------------" << endl;
	for (int size = 600; size <= 3000; size += 400)
	{
		cout << endl
			 << "Running OnMultLine with size " << size << endl
			 << endl;
		OnMultLineOuterLoop(size, size);
	}
	for (int size = 4096; size <= 10240; size += 2048)
	{
		cout << endl
			 << "Running OnMultLine with size " << size << endl
			 << endl;

		OnMultLineOuterLoop(size, size);
	}

	cout << "---------------- Executing Line Multiplication Parallel InnerLoop ----------------" << endl;
	for (int size = 600; size <= 3000; size += 400)
	{
		cout << endl
			 << "Running OnMultLine with size " << size << endl
			 << endl;

		OnMultLineOuterLoop(size, size);
	}
	for (int size = 4096; size <= 10240; size += 2048)
	{
		cout << endl
			 << "Running OnMultLine with size " << size << endl
			 << endl;

		OnMultLineOuterLoop(size, size);
	}

	fclose(stdout);

	return 0;
}
#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <cstdlib>
#include <fstream>
#include <chrono>
#include <papi.h>

using namespace std;
using namespace std::chrono;

#define SYSTEMTIME high_resolution_clock::time_point

void OnMult(int m_ar, int m_br)
{

	SYSTEMTIME Time1, Time2;

	char st[100];
	double temp, *pha, *phb, *phc;
	int i, j, k;

	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	Time1 = high_resolution_clock::now();

	for (i = 0; i < m_ar; i++)
	{
		for (j = 0; j < m_br; j++)
		{
			temp = 0;
			for (k = 0; k < m_ar; k++)
			{
				temp += pha[i * m_ar + k] * phb[k * m_br + j];
			}
			phc[i * m_ar + j] = temp;
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

// add code here for line x line matriz multiplication

void OnMultLine(int m_ar, int m_br)
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

// add code here for block x block matriz multiplication
void OnMultBlock(int m_ar, int m_br, int bkSize)
{
	SYSTEMTIME Time1, Time2;

	char st[100];
	int i, j, k, ii, jj, kk;

	double *matrixA = new double[m_ar * m_ar];
	double *matrixB = new double[m_ar * m_ar];
	double *matrixC = new double[m_ar * m_ar];

	for (i = 0; i < m_ar; i++)
	{
		for (j = 0; j < m_ar; j++)
		{
			matrixA[i * m_ar + j] = 1.0;
		}
	}

	for (i = 0; i < m_br; i++)
	{
		for (j = 0; j < m_br; j++)
		{
			matrixB[i * m_br + j] = i + 1;
		}
	}

	Time1 = high_resolution_clock::now();

	for (i = 0; i < m_ar; i += bkSize)
	{
		for (j = 0; j < m_br; j += bkSize)
		{
			for (k = 0; k < m_ar; k += bkSize)
			{
				for (ii = i; ii < i + bkSize; ii++)
				{
					for (jj = j; jj < j + bkSize; jj++)
					{
						for (kk = k; kk < k + bkSize; kk++)
						{
							matrixC[ii * m_br + jj] += matrixA[ii * m_ar + kk] * matrixB[kk * m_br + jj];
						}
					}
				}
			}
		}
	}

	Time2 = high_resolution_clock::now();
	sprintf(st, "Time: %3.3f seconds\n", duration_cast<duration<double>>(Time2 - Time1).count());
	cout << st;

	// Display 10 elements of the result matrix to verify correctness
	cout << "Result matrix: " << endl;
	for (i = 0; i < 1; i++)
	{
		for (j = 0; j < min(10, m_br); j++)
		{
			cout << matrixC[j] << " ";
		}
	}
	cout << endl;

	delete[] matrixA;
	delete[] matrixB;
	delete[] matrixC;
}

void handle_error(int retval)
{
	printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
	exit(1);
}

void init_papi()
{
	int retval = PAPI_library_init(PAPI_VER_CURRENT);
	if (retval != PAPI_VER_CURRENT && retval < 0)
	{
		printf("PAPI library version mismatch!\n");
		exit(1);
	}
	if (retval < 0)
		handle_error(retval);

	std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
			  << " MINOR: " << PAPI_VERSION_MINOR(retval)
			  << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}

int main(int argc, char *argv[])
{

	freopen("outputCPP.txt", "w", stdout);

	int EventSet = PAPI_NULL;
	long long values[2];
	int ret;

	// PAPI Initialization
	init_papi();

	ret = PAPI_create_eventset(&EventSet);
	if (ret != PAPI_OK)
		cout << "ERROR: create eventset" << endl;

	ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
	if (ret != PAPI_OK)
		cout << "ERROR: PAPI_L1_DCM" << endl;

	ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
	if (ret != PAPI_OK)
		cout << "ERROR: PAPI_L2_DCM" << endl;

	// Execute OnMult for matrix sizes 600x600 to 3000x3000 with increments of 400
	cout << "---------------- Executing Standard Multiplication ----------------" << endl;
	for (int size = 600; size <= 3000; size += 400)
	{
		cout << endl
			 << "Running OnMult with size " << size << endl
			 << endl;
		// Start counting
		ret = PAPI_start(EventSet);
		if (ret != PAPI_OK)
			cout << "ERROR: Start PAPI" << endl;

		OnMult(size, size);

		ret = PAPI_stop(EventSet, values);
		if (ret != PAPI_OK)
			cout << "ERROR: Stop PAPI" << endl;
		printf("L1 DCM: %lld \n", values[0]);
		printf("L2 DCM: %lld \n", values[1]);

		ret = PAPI_reset(EventSet);
		if (ret != PAPI_OK)
			std::cout << "FAIL reset" << endl;
	}

	// Execute OnMultLine for matrix sizes 600x600 to 3000x3000 with increments of 400
	// And then for sizes 4096x4096 to 10240x10240 with intervals of 2048
	cout << "---------------- Executing Line Multiplication ----------------" << endl;
	for (int size = 600; size <= 3000; size += 400)
	{
		cout << endl
			 << "Running OnMultLine with size " << size << endl
			 << endl;
		ret = PAPI_start(EventSet);
		if (ret != PAPI_OK)
			cout << "ERROR: Start PAPI" << endl;
		OnMultLine(size, size);

		ret = PAPI_stop(EventSet, values);
		if (ret != PAPI_OK)
			cout << "ERROR: Stop PAPI" << endl;
		printf("L1 DCM: %lld \n", values[0]);
		printf("L2 DCM: %lld \n", values[1]);

		ret = PAPI_reset(EventSet);
		if (ret != PAPI_OK)
			std::cout << "FAIL reset" << endl;
	}
	for (int size = 4096; size <= 10240; size += 2048)
	{
		cout << endl
			 << "Running OnMultLine with size " << size << endl
			 << endl;

		ret = PAPI_start(EventSet);
		if (ret != PAPI_OK)
			cout << "ERROR: Start PAPI" << endl;
		OnMultLine(size, size);

		ret = PAPI_stop(EventSet, values);
		if (ret != PAPI_OK)
			cout << "ERROR: Stop PAPI" << endl;
		printf("L1 DCM: %lld \n", values[0]);
		printf("L2 DCM: %lld \n", values[1]);

		ret = PAPI_reset(EventSet);
		if (ret != PAPI_OK)
			std::cout << "FAIL reset" << endl;
	}

	// Execute OnMultBlock for matrix sizes 4096x4096 to 10240x10240 with intervals of 2048
	// For each size, perform multiplication with block sizes of 128, 256, and 512
	cout << "---------------- Executing Block Multiplication ----------------" << endl;
	int blockSizes[] = {128, 256, 512};
	for (int size = 4096; size <= 10240; size += 2048)
	{
		for (int bkSize : blockSizes)
		{
			cout << endl
				 << "Running OnMultBlock with size " << size << " and block size " << bkSize << endl
				 << endl;

			ret = PAPI_start(EventSet);
			if (ret != PAPI_OK)
				cout << "ERROR: Start PAPI" << endl;

			OnMultBlock(size, size, bkSize);
			ret = PAPI_stop(EventSet, values);
			printf("L1 DCM: %lld \n", values[0]);
			printf("L2 DCM: %lld \n", values[1]);
			ret = PAPI_reset(EventSet);
		}
	}

	ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
	if (ret != PAPI_OK)
		std::cout << "FAIL remove event" << endl;

	ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
	if (ret != PAPI_OK)
		std::cout << "FAIL remove event" << endl;

	ret = PAPI_destroy_eventset(&EventSet);
	if (ret != PAPI_OK)
		std::cout << "FAIL destroy" << endl;

	fclose(stdout);

	return 0;
}
#!/bin/bash

curl 'https://push2.eastmoney.com/api/qt/ulist/get?invt=3&pi=0&pz=7&mpi=2000&fields=f2,f3,f8,f12&po=1&_=1678947675181&secids=0.002252,0.002270,1.688100,1.600699,0.300342,0.002979,1.603638' > e.txt 


sh table.sh

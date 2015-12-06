#!/bin/sh

#PBS -N AR_ZAD2
#PBS -q l_short
#PBS -A plgmorgaroth2015b

cd /people/plgmorgaroth/AR/zad2

module add mvapich2
module add mpiexec

NAME=$(date +%s | sha256sum | base64 | head -c 5 ; echo)

rm -f "doit${NAME}"
mpicc -g doit.c -o "doit${NAME}"
chmod +x "doit${NAME}"

mpiexec -np ${PROCS} "doit${NAME}" $STARS $PROMIEN >> "${STARS}_${PROCS}.log"
rm -f "doit${NAME}"
rm -rf core*
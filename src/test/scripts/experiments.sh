#!/bin/bash

# kill children when we die
trap "trap - SIGTERM && kill -- -$$" SIGINT SIGTERM EXIT

BASE=/home/experiments/project/
OPTIMISE="${BASE}/src/test/python/optimise.py --verbose"
BENCHMARK="${BASE}/src/test/python/benchmark.py --verbose"

EXTENDED_CORPUS="--exclude canterbury/artificial/* unit_tests/* mixed_language/cedict.txt binary/medium-git canterbury/large/* resource_consumption/*"
STANDARD_CORPUS="${EXTENDED_CORPUS} training/*"
TEXT_TEST_CORPUS="--include canterbury/canterbury/*.txt canterbury/canterbury/cp.html canterbury/canterbury/fields.c canterbury/canterbury/grammar.lsp canterbury/canterbury/xargs.1 single_language/* single_language/genji/* mixed_language/creativecommonsukranian.html"

OPTIMISATION_FILES="canterbury/canterbury/*"
OPTIMISATION_DEPTH=5
OPTIMISATION_PRIOR=uniform_byte
OPTIMISATION_GRANULARITIES="0 5 10 20"

OPTIMISATION_MODELS=""
for granularity in $OPTIMISATION_GRANULARITIES; do
	OPTIMISATION_MODELS="${OPTIMISATION_MODELS} ppm_optimal_parameters:depths=${OPTIMISATION_DEPTH}:granularity=${granularity}:prior=${OPTIMISATION_PRIOR}"
done
#${OPTIMISE} ${OPTIMISATION_MODELS} --include ${OPTIMISATION_FILES} 
#cd ${BASE}/experiments/tables; (for granularity in 0 5 10 20; do awk -F, -v OFS=',' "{print \$1, \$2, ${granularity}, \$3, \$4, \$5, \$6}" ppm_optimal_parameters\:depths\=5\:granularity\=${granularity}:prior=uniform_byte.csv; done) > cat.csv

PARAMETER_DEPTHS=0,1,2,3,4,5,6,7,8,9
PARAMETER_PRIORS="uniform_byte uniform_token polya_token polya_stoken_uniform_token polya_stoken_uniform_byte"
PARAMETER_GRANULARITY=10

PARAMETER_MODELS=""
for prior in $PARAMETER_PRIORS; do
	PARAMETER_MODELS="${PARAMETER_MODELS} ppm_optimal_alpha_beta:depths=${PARAMETER_DEPTHS}:granularity=${PARAMETER_GRANULARITY}:prior=${prior} ppm_optimal_parameters:depths=${PARAMETER_DEPTHS}:granularity=${PARAMETER_GRANULARITY}:prior=${prior}"
done
#${OPTIMISE} ${PARAMETER_MODELS} ${EXTENDED_CORPUS}

# Contour plots

function contour_plot {
	fname=$1
	pretty=$2
	ub_depth=$3
	ut_depth=$4
	pt_depth=$5
	${OPTIMISE} ppm_contour_plot:prior=uniform_byte:depth=${ub_depth} ppm_contour_plot:prior=uniform_token:depth=${ut_depth} ppm_contour_plot:prior=polya_token:depth=${pt_depth} --include ${fname} --num-workers 3 2>&1 | sed -e "s/^/${pretty}: /" | tee log/contour_${pretty}&
}
#contour_plot canterbury/canterbury/alice29.txt alice 4 4 4
#contour_plot canterbury/canterbury/lcet10.txt lcet 4 4 4
#contour_plot binary/small-xscreensaver xscreensaver 6 6 6
#contour_plot single_language/genji/all.txt genji 6 3 3
#contour_plot single_language/kokoro.txt kokoro 6 3 3
#contour_plot mixed_language/creativecommonsukranian.html cc 8 4 4
wait

#${OPTIMISE} ppm_multi_contour_plot:prior=uniform_byte:depth=6 ${TEXT_TEST_CORPUS} 

# Multi-file parameter optimisation

MULTI_PARAMETER_MODELS=""
for prior in $PARAMETER_PRIORS; do
	MULTI_PARAMETER_MODELS="${MULTI_PARAMETER_MODELS} ppm_multi_optimal_alpha_beta:depths=${PARAMETER_DEPTHS}:granularity=${PARAMETER_GRANULARITY}:prior=${prior}"
done
#cd $BASE/experiments/tables
#${OPTIMISE} ${MULTI_PARAMETER_MODELS} --include training/* 
#for model in $MULTI_PARAMETER_MODELS; do
#	mv ${model}.csv training_${model}.csv
#done
#${OPTIMISE} ${MULTI_PARAMETER_MODELS} ${TEXT_TEST_CORPUS} 
#for model in $MULTI_PARAMETER_MODELS; do
#	mv ${model}.csv test_${model}.csv
#done

# Reference models
BASE_MODELS="uniform_byte uniform_token polya_token polya_stoken_uniform_byte polya_stoken_uniform_token"
SINGLE_MODELS=""
CRP_MODELS=""
LZW_MODELS="none_lzw_byte"
PPM_MODELS=""
for base in ${BASE_MODELS}; do
	SINGLE_MODELS="${SINGLE_MODELS} none_${base}"
	CRP_MODELS="${SINGLE_MODELS} crp_${base}"
	LZW_MODELS="${SINGLE_MODELS} lzw_${base}"
	PPM_MODELS="${PPM_MODELS} ppm_training_group_opt_${base} ppm_test_group_opt_${base}"
done
#TODO: update once you know optimal depth for the new Polya models 
PPM_MODELS="ppm6_uniform_byte ppm4_uniform_token ppm4_polya_token ppm4_polya_stoken_uniform_token ppm4_polya_stoken_uniform_byte ppm5_uniform_byte ppm5_uniform_token ppm5_polya_token ppm5_polya_stoken_uniform_byte ppm5_polya_stoken_uniform_token"
PPM_DETAILED_MODELS=""
for d in 1 2 3 4 5 6 7 8 9; do
	for src in training test; do
		for base in ${BASE_MODELS}; do
			PPM_DETAILED_MODELS="${PPM_DETAILED_MODELS} ppm_${src}_group_${d}_${base}"
		done
	done
done
REFERENCE_MODELS="ref_bzip2 ref_cmix ref_compress ref_gzip ref_lzma ref_paq8hp12 ref_PPMd ref_SCSU ref_zpaq"
${BENCHMARK} ${SINGLE_MODELS} ${CRP_MODELS} ${LZW_MODELS} ${PPM_MODELS} ${PPM_DETAILED_MODELS} ${REFERENCE_MODELS} ${EXTENDED_CORPUS} --csv ${BASE}/experiments/tables/benchmark.csv --rerun-errors

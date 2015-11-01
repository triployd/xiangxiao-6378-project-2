#!/bin/bash
#
# Preliminary version of the grading script for AOS Project 2
#
# This script will check that you are producing output files in the proper format.
# It does not check whether the content of your output is correct.
# 
# Syntax: 
# 
#  > bash grade.sh [relative path to config file]
#
#
#
#


CONFIG=$2

# extract the important lines from the config file
sed -e "s/#.*//" $CONFIG | sed -e "/^\s*$/d" > temp
# insert a new line to EOF # necessary for the while loop
echo  >> temp

netid=""
node_count=0
nodes_location="" #Stores a # delimited string of Location of each node
host_names=() #Stores the hostname of each node
path_dict=() # Stores the Token path of each node
num_tokens_configured=0 # Store the number of tokens in the config file

current_line=1
while read line; 
do
	#echo $line;
########Extract Number of nodes and Login ID
	if [ $current_line -eq 1 ]; then
		#number of nodes
		node_count=$(echo $line | cut -f1 -d" ")
		#convert it to an integer
  		let node_count=node_count+0 
  	fi
  	let current_line+=1
done < temp




echo -e "############################"
echo -e "# AUTOMATED GRADING SCRIPT #"
echo -e "#        PROJECT 2         #"
echo -e "############################"
echo -e "\nGrading using config file $CONFIG\n"

shopt -s nocasematch

# open the files and parse 
outname="${CONFIG%.*}-"
unset timestamps
declare -A timestamps  # indexed as: timestamps[i,j,k] where i,j,k as in project description 



# read all the output files
for node_id in $(seq 0 $(expr $node_count - 1)); do
    if [ ! -f "${outname}$node_id.out" ]; then
	echo "File ${outname}$node_id.out does not exist!"
	break
    fi
    sed -e "/^\s*$/d" "${outname}$node_id.out" > temp 
    echo  >> temp
    
    line_num=0;
    while read line; do
	arr=($line)
	for ((i=0; i<${#arr[*]}; i++)); do
	    timestamps["$node_id.$line_num.$i"]=${arr[i]}
	done
	line_num=$(expr $line_num + 1)
	if [ ${#arr[*]} -ne $node_count ]; then
	    echo "WARNING: File ${outname}$node_id.out only has ${#arr[*]} of $node_count entries at for vector $line_num."
	fi
    done < temp
    num_snapshots=$line_num
done

for i in $(seq 0 $(expr $node_count - 1)); do
    echo "FILE: ${outname}$i.out"
    for j in $(seq 0 $num_snapshots); do
	for k in $(seq 0 $(expr $node_count - 1)); do
	    echo -n "${timestamps[$i.$j.$k]} "
	done
	echo "" # newline
    done
done



DIR="$( cd "$( dirname "$0" )" && pwd )"
JAR_PATH="$DIR/conf/:$DIR/build/libs/version2.jar"
MACHINE_LIST="$DIR/conf/machine_list_large"
SCRIPT="java -cp $JAR_PATH cs455.scaling.client.Client sacramento 45960"
COMMAND='gnome-terminal --geometry=200x40'
for machine in `cat $MACHINE_LIST`
do
 OPTION='--tab -t "'$machine'" -e "ssh -t '$machine' cd '$DIR'; echo '$SCRIPT'; '$SCRIPT'"'
 COMMAND+=" $OPTION"
done
eval $COMMAND &


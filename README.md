# MazePlugin
##在你的服务器生成迷宫！
###享受走迷宫寻宝的冒险吧～
###支持在服务器端自动生成随机的迷宫，迷宫内会刷出宝物和怪物，你需要做的就是，穿过路上无数怪物，从起点走到迷宫的对角的出口。
##玩法说明：
###/StartMazeTour 开始一次迷宫旅行
###/Abort 放弃，玩家被传送会出生点，所有物品剥夺
###/Finish 当玩家抵达起点对角的终点后使用，本轮游戏结束，所有玩家传送回出生点位，其中第一个玩家和他的伙伴获得所有身上的宝物，
###其他人身上的物品有70%几率小时
###/GenMaze 管理员命令，立即重设迷宫

###plugin.yml设置：
###lastWidthX : 32 //没什么卵用
###lastWidthY : 32 //没什么卵用
###currentWidthX : 64  //迷宫长度
###currentWidthY : 64  //迷宫宽度 ，只支持正方形迷宫，换算到mc要乘以6倍才是方块数
###OriginX : 100000 //迷宫生成起点X
###OriginY : 192    //迷宫生成起点Y
###OriginZ : 100000 //迷宫生成起点Z

##构建
###安装maven
###mvn package assembly:single

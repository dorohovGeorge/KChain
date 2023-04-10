import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom
import kotlin.concurrent.thread

const val seconds = 1000L

var mutex = Mutex()


const val delayTimeForPickWinner = 15L

val announcements = mutableListOf<String>()

var blockchain = Blockchain()

fun main() =
    runBlocking {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind("localhost", 9002)
        println("Server is listening at ${serverSocket.localAddress}")
        while (true) {
            val socket = serverSocket.accept()
            println("Accepted $socket")

            launch {
                for (candidateBlock in blockchain.candidatesBlock) {
                    mutex.lock()
                    blockchain.addInTempBlocks(candidateBlock)
                    mutex.unlock()
                }


            }

            //pickWinner
            launch {
                pickWinner()
            }

            launch {
                connLogic(socket)
            }


        }
    }


suspend fun connLogic(socket: Socket) = coroutineScope {
    val receiveChannel = socket.openReadChannel()
    val sendChannel = socket.openWriteChannel(autoFlush = true)
    var address = ""

    launch {
        sendChannel.writeStringUtf8("Enter token balance\n")

        try {
            while (true) {
                val balance = receiveChannel.readUTF8Line()
                address = LocalDateTime.now().toString().hash()
                if (balance == null) {
                    println("$balance not a number")
                    withContext(Dispatchers.IO) {
                        socket.close()
                    }
                } else {
                    blockchain.validators[address] = balance.toInt()
                    println(blockchain.validators)
                }
                //sendChannel.writeStringUtf8("Hello, $name!\n")
                break
            }
        } catch (e: Throwable) {
            withContext(Dispatchers.IO) {
                socket.close()
            }
        }

        sendChannel.writeStringUtf8("Enter money balance\n")


        try {
            while (true) {
                val money = receiveChannel.readUTF8Line()
                if (money == null) {
                    println("$money not a number")
                    blockchain.validators.remove(address)
                    withContext(Dispatchers.IO) {
                        socket.close()
                    }
                }

                mutex.lock()
                val oldLastIndex = blockchain.getOldLastBlock()
                mutex.unlock()

                val newBlock = blockchain.generateBlock(oldLastIndex, money!!.toInt(), address)

                if (blockchain.isBlockValid(oldLastIndex, newBlock)) {
                    blockchain.addCandidateBlocks(newBlock)
                }
                sendChannel.writeStringUtf8("Enter money balance\n")
            }
        } catch (e: Throwable) {
            withContext(Dispatchers.IO) {
                socket.close()
            }
        }
    }
    launch {
        while (true) {
            delay((delayTimeForPickWinner + 1) * seconds)
            mutex.lock()
            var output = blockchain.toString()
            if (announcements.isNotEmpty()) {
                sendChannel.writeStringUtf8(announcements.toString())
                announcements.clear()
            }
            mutex.unlock()

            sendChannel.writeStringUtf8("$output\n")
        }

    }
}


suspend fun pickWinner() {

    delay(delayTimeForPickWinner * seconds)
    println("pickWinner")
    mutex.lock()
    val tempBlocks = blockchain.getTempBlocks()
    mutex.unlock()

    val lotteryPool = mutableListOf<String>()
    //var flag = true
    if (tempBlocks.size > 0) {
        run OUTER@{
            for (block in tempBlocks) {
                for (node in lotteryPool) {
                    if (block.isValidatorNull()) {
                        return@OUTER
                    }
                }

                mutex.lock()
                val setValidators = blockchain.validators
                mutex.unlock()

                if (setValidators[block.validator] != null) {
                    val count = setValidators[block.validator]
                    repeat((0..count!!).count()) {
                        lotteryPool.add(block.validator)
                    }
                }
            }

            val winnerNumber = ThreadLocalRandom.current().nextLong(0, lotteryPool.size.toLong()).toInt()
            val winner = lotteryPool[winnerNumber]

            for (block in tempBlocks) {
                if (block.validator == winner) {
                    mutex.lock()
                    blockchain.addBlocks(block)
                    mutex.unlock()
                    for (i in blockchain.validators) {
                        announcements.add("\nwinning validator: $winner \n")
                    }
                    break
                }
            }
        }

        mutex.lock()
        blockchain.makeTempBlocksEmpty()
        mutex.unlock()
    }
}

import java.time.LocalDateTime

class Blockchain {
    private var blocks: MutableList<Block> = mutableListOf()
    var candidatesBlock: MutableList<Block> = mutableListOf()
    private var tempBlocks: MutableList<Block> = mutableListOf()

    var validators: MutableMap<String, Int> = mutableMapOf()

    init {
        val genesisBlock = Block(0, LocalDateTime.now().toString(), 0, "", "", "")
        println(genesisBlock)
        blocks.add(genesisBlock)
    }


    fun generateBlock(oldBlock: Block, money: Int, address: String): Block {
        return Block(oldBlock.id + 1, LocalDateTime.now().toString(), money, "", oldBlock.hash, address)
    }

    fun isBlockValid(oldBlock: Block, newBlock: Block): Boolean {
        when {
            oldBlock.id + 1 != newBlock.id -> return false
            else -> {
                when {
                    newBlock.hash != newBlock.calculateHash() -> return false
                    newBlock.prevHash != oldBlock.calculateHash() -> return false
                }
                return true
            }
        }
    }

    fun addInTempBlocks(block: Block) {
        this.tempBlocks.add(block)
    }

    fun getTempBlocks(): MutableList<Block> = this.tempBlocks

    fun addBlocks(block: Block) = this.blocks.add(block)

    suspend fun addCandidateBlocks(candidateBlock: Block) {
        this.candidatesBlock.add(candidateBlock)
        for (tmp in blockchain.candidatesBlock) {
            mutex.lock()
            blockchain.addInTempBlocks(tmp)
            mutex.unlock()
        }
    }

    fun makeTempBlocksEmpty() {
        this.tempBlocks = mutableListOf()
    }

    fun getOldLastBlock(): Block = this.blocks[this.blocks.size - 1]

    override fun toString(): String {
        return this.blocks.toString()
    }
}
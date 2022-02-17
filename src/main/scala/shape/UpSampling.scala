package shape

import spinal.core._
import spinal.lib._
import wa.WaCounter

case class UpSamplingConfig(DATA_WIDTH: Int, COMPUTE_CHANNEL_NUM: Int, FEATURE: Int, CHANNEL_WIDTH: Int, ROW_MEM_DEPTH: Int) {
    val STREAM_DATA_WIDTH = DATA_WIDTH * COMPUTE_CHANNEL_NUM
    val FEATURE_WIDTH = log2Up(FEATURE)
    val channelMemDepth = 512 / COMPUTE_CHANNEL_NUM //最多支持512通道
}

class UpSampling(upSamplingConfig: UpSamplingConfig) extends Component {
    //    val io = new Bundle{
    //        val sData = slave Stream UInt(upSamplingConfig.STREAM_DATA_WIDTH bits)
    //        val mData = master Stream UInt(upSamplingConfig.STREAM_DATA_WIDTH bits)
    //        val rowNumIn = in UInt (upSamplingConfig.FEATURE_WIDTH bits)
    //        val colNumIn = in UInt (upSamplingConfig.FEATURE_WIDTH bits)
    //        val channelIn = in UInt (upSamplingConfig.CHANNEL_WIDTH bits)
    //    }
    val io = ShapePort(upSamplingConfig.STREAM_DATA_WIDTH, upSamplingConfig.FEATURE_WIDTH, upSamplingConfig.CHANNEL_WIDTH)
    noIoPrefix()

    val computeChannelTimes = io.channelIn >> log2Up(upSamplingConfig.COMPUTE_CHANNEL_NUM)
    val computeColumn = io.colNumIn << 1
    val computeRow = io.rowNumIn << 1

    val channelCnt = WaCounter(io.mData.fire, upSamplingConfig.CHANNEL_WIDTH, computeChannelTimes - 1)
    val columnCnt = WaCounter(channelCnt.valid, computeColumn.getWidth, computeColumn - 1)
    val rowCnt = WaCounter(channelCnt.valid && columnCnt.valid, computeRow.getWidth, computeRow - 1)

    val dataTemp = StreamFifo(UInt(upSamplingConfig.STREAM_DATA_WIDTH bits), upSamplingConfig.ROW_MEM_DEPTH)
    dataTemp.io.push <> io.sData

    val channelMem = StreamFifo(UInt(upSamplingConfig.STREAM_DATA_WIDTH bits), upSamplingConfig.channelMemDepth)

    when(!columnCnt.count(0)){
        io.mData<>dataTemp.io.pop
        channelMem.io.push.payload <> dataTemp.io.pop.payload
        channelMem.io.push.valid <> dataTemp.io.pop.valid
        channelMem.io.pop.ready := False
    } otherwise{
        io.mData <> channelMem.io.pop
        dataTemp.io.pop.ready := False
        channelMem.io.push.valid := False
        channelMem.io.push.payload := 0
    }

}

object UpSampling extends App {
    SpinalVerilog(new UpSampling(UpSamplingConfig(8,8,640,10,1024)))
}

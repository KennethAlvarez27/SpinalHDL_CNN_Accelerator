import config.Config
import misc.TotalTcl
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._
import config.Config._
import conv.compute._
import shape._
import wa.dma.{DmaConfig, DmaRead, DmaWrite}

import java.io.File


class Npu(convConfig: ConvConfig, shapeConfig: ShapeConfig) extends Component {
    val firstLayerWidth = if (imageType.dataType == imageType.rgb) 4 * convConfig.DATA_WIDTH else 8

    val regSData = slave(AxiLite4(log2Up(registerAddrSize), 32))
    AxiLite4SpecRenamer(regSData)

    val conv = List.fill(Config.ConvNum)(new Conv(convConfig))
    val shape = new Shape(shapeConfig)
    val register = new instruction.Instruction(Config.instructionAddr, Config.instructionType)
    if (!Config.useXilinxDma) {
        //        val io = new Bundle {
        //            val convSData = master(Axi4ReadOnly(Axi4Config(log2Up(DDRSize), convConfig.FEATURE_S_DATA_WIDTH, useQos = false, useId = false, useRegion = false, useLock = false, wUserWidth = 0, awUserWidth = 0, bUserWidth = 0)))
        //            val convFirstLayerSData = master(Axi4ReadOnly(Axi4Config(log2Up(DDRSize), firstLayerWidth, useQos = false, useId = false, useRegion = false, useLock = false, wUserWidth = 0, awUserWidth = 0, bUserWidth = 0)))
        //            val convMData = master(Axi4WriteOnly(Axi4Config(log2Up(DDRSize), convConfig.FEATURE_M_DATA_WIDTH, useQos = false, useId = false, useRegion = false, useLock = false, wUserWidth = 0, awUserWidth = 0, bUserWidth = 0)))
        //            val shapeSData = master(Axi4ReadOnly(Axi4Config(log2Up(DDRSize), shapeConfig.STREAM_DATA_WIDTH, useQos = false, useId = false, useRegion = false, useLock = false, wUserWidth = 0, awUserWidth = 0, bUserWidth = 0)))
        //            val shapeSData1 = master(Axi4ReadOnly(Axi4Config(log2Up(DDRSize), shapeConfig.STREAM_DATA_WIDTH, useQos = false, useId = false, useRegion = false, useLock = false, wUserWidth = 0, awUserWidth = 0, bUserWidth = 0)))
        //            val shapeMData = master(Axi4WriteOnly(Axi4Config(log2Up(DDRSize), shapeConfig.STREAM_DATA_WIDTH, useQos = false, useId = false, useRegion = false, useLock = false, wUserWidth = 0, awUserWidth = 0, bUserWidth = 0)))
        //
        //        }
        //        noIoPrefix()
        //        Axi4SpecRenamer(io.convSData)
        //        Axi4SpecRenamer(io.convFirstLayerSData)
        //        Axi4SpecRenamer(io.convMData)
        //        Axi4SpecRenamer(io.shapeSData)
        //        Axi4SpecRenamer(io.shapeSData1)
        //        Axi4SpecRenamer(io.shapeMData)
        //
        //        val convDmaWrite = new DmaWrite(DmaConfig(log2Up(DDRSize), convConfig.FEATURE_S_DATA_WIDTH, 8, burstSize))
        //        val convDmaRead = new DmaRead(DmaConfig(log2Up(DDRSize), convConfig.FEATURE_M_DATA_WIDTH, 8, burstSize))
        //        val convFirstLayerDmaRead = new DmaRead(DmaConfig(log2Up(DDRSize), firstLayerWidth, 8, burstSize))
        //        convDmaWrite.io.M_AXI_S2MM <> io.convMData
        //        convDmaRead.io.M_AXI_MM2S <> io.convSData
        //        convFirstLayerDmaRead.io.M_AXI_MM2S <> io.convFirstLayerSData
        //
        //        val shapeDmaWrite = new DmaWrite(DmaConfig(log2Up(DDRSize), shapeConfig.STREAM_DATA_WIDTH, 8, burstSize))
        //        val shapeDmaRead = new DmaRead(DmaConfig(log2Up(DDRSize), shapeConfig.STREAM_DATA_WIDTH, 8, burstSize))
        //        val shapeDmaRead1 = new DmaRead(DmaConfig(log2Up(DDRSize), shapeConfig.STREAM_DATA_WIDTH, 8, burstSize))
        //        shapeDmaWrite.io.M_AXI_S2MM <> io.shapeMData
        //        shapeDmaRead.io.M_AXI_MM2S <> io.shapeSData
        //        shapeDmaRead1.io.M_AXI_MM2S <> io.shapeSData1
        //
        //        conv.io.sData <> convDmaRead.io.M_AXIS_MM2S
        //        StreamWidthAdapter(convFirstLayerDmaRead.io.M_AXIS_MM2S, conv.io.sFeatureFirstLayerData)
        //        convDmaWrite.io.M_AXIS_S2MM <> conv.io.mData
        //        conv.io.dmaWriteValid <> convDmaWrite.io.cmd.valid
        //        conv.io.dmaReadValid <> convDmaRead.io.cmd.valid
        //        conv.io.dmaFirstLayerReadValid <> convFirstLayerDmaRead.io.cmd.valid
        //        conv.io.introut <> convDmaWrite.io.cmd.introut
        //        shape.io.sData(0) <> shapeDmaRead.io.M_AXIS_MM2S
        //        shape.io.sData(1) <> shapeDmaRead1.io.M_AXIS_MM2S
        //        shape.io.mData <> shapeDmaWrite.io.M_AXIS_S2MM
        //        shape.io.dmaReadValid(0) <> shapeDmaRead.io.cmd.valid
        //        shape.io.dmaReadValid(1) <> shapeDmaRead1.io.cmd.valid
        //        shape.io.dmaWriteValid <> shapeDmaWrite.io.cmd.valid
        //        shape.io.introut <> shapeDmaWrite.io.cmd.introut
        //        register.dma(0)(0)(0).asUInt <> convDmaWrite.io.cmd.addr
        //        register.dma(0)(1)(0).asUInt <> convDmaWrite.io.cmd.len
        //        register.dma(0)(0)(1).asUInt <> convDmaRead.io.cmd.addr
        //        register.dma(0)(1)(1).asUInt <> convDmaRead.io.cmd.len
        //        register.dma(0)(0)(1).asUInt <> convFirstLayerDmaRead.io.cmd.addr
        //        if (Config.imageType.dataType == Config.imageType.gray) {
        //            convFirstLayerDmaRead.io.cmd.len := (register.dma(0)(1)(1).asUInt >> 2).resize(32 bits)
        //        } else {
        //            register.dma(0)(1)(1).asUInt <> convFirstLayerDmaRead.io.cmd.len
        //        }
        //        register.dma(1)(0)(0).asUInt <> shapeDmaWrite.io.cmd.addr
        //        register.dma(1)(1)(0).asUInt <> shapeDmaWrite.io.cmd.len
        //        register.dma(1)(0)(1).asUInt <> shapeDmaRead.io.cmd.addr
        //        register.dma(1)(1)(1).asUInt <> shapeDmaRead.io.cmd.len
        //        register.dma(1)(0)(2).asUInt <> shapeDmaRead1.io.cmd.addr
        //        register.dma(1)(1)(2).asUInt <> shapeDmaRead1.io.cmd.len
    } else {

        case class Cmd() extends Bundle {
            val cmd = out Bits (64 bits)
            val valid = out Bool()
            val introut = in Bool()
        }
        class genConvIo(i: Int) extends Area {
            val convSData = slave Stream UInt(convConfig.FEATURE_S_DATA_WIDTH bits) setName ("convSData" + i.toString)
            val convFirstLayerSData = slave Stream UInt(firstLayerWidth bits) setName ("convFirstLayerSData" + i.toString)
            val convMData = master Stream UInt(convConfig.FEATURE_M_DATA_WIDTH bits) setName ("convMData" + i.toString)
            val convSDataCmd = Cmd() setName ("convSDataCmd" + i.toString)
            val convFirstLayerSDataCmd = Cmd() setName ("convFirstLayerSDataCmd" + i.toString)
            val convMDataCmd = Cmd() setName ("convMDataCmd" + i.toString)
            val convMLast = out Bool() setName ("convMLast" + i.toString)

        }


        val io = new Bundle {

            val convIo = Array.tabulate(ConvNum)(i => {
                val gen = new genConvIo(i)
                gen
            })

            val shapeSData = slave Stream UInt(shapeConfig.STREAM_DATA_WIDTH bits) setName ("shapeSData")
            val shapeSData1 = slave Stream UInt(shapeConfig.STREAM_DATA_WIDTH bits) setName ("shapeSData1")
            val shapeMData = master Stream UInt(shapeConfig.STREAM_DATA_WIDTH bits) setName ("shapeMData")


            val shapeSDataCmd = Cmd() setName ("shapeSDataCmd")
            val shapeSData1Cmd = Cmd() setName ("shapeSData1Cmd")
            val shapeMDataCmd = Cmd() setName ("shapeMDataCmd")
            val shapeMLast = out Bool() setName ("shapeMLast")


        }
        noIoPrefix()

        (0 until ConvNum).foreach(i => {
            conv(i).io.last <> io.convIo(i).convMLast
            io.convIo(i).convMDataCmd.valid := conv(i).io.dmaWriteValid
            io.convIo(i).convSDataCmd.valid := conv(i).io.dmaReadValid
            io.convIo(i).convFirstLayerSDataCmd.valid := conv(i).io.dmaFirstLayerReadValid
            io.convIo(i).convSData <> conv(i).io.sData
            io.convIo(i).convMData <> conv(i).io.mData
            io.convIo(i).convFirstLayerSData <> conv(i).io.sFeatureFirstLayerData
            conv(i).io.introut := io.convIo(i).convMDataCmd.introut

            io.convIo(i).convMDataCmd.cmd := register.dma(i)(0)(0) ## register.dma(i)(1)(0)
            io.convIo(i).convSDataCmd.cmd := register.dma(i)(0)(1) ## register.dma(0)(1)(1)
            io.convIo(i).convFirstLayerSDataCmd.cmd := register.dma(i)(0)(1) ## register.dma(i)(1)(1)
        })

        shape.io.last <> io.shapeMLast


        io.shapeMDataCmd.valid := shape.io.dmaWriteValid
        io.shapeMDataCmd.cmd := register.dma(1)(0)(0) ## register.dma(1)(1)(0)
        io.shapeSDataCmd.valid := shape.io.dmaReadValid(0)
        io.shapeSDataCmd.cmd := register.dma(1)(0)(1) ## register.dma(1)(1)(1)
        io.shapeSData1Cmd.valid := shape.io.dmaReadValid(1)
        io.shapeSData1Cmd.cmd := register.dma(1)(0)(2) ## register.dma(1)(1)(2)


        io.shapeSData <> shape.io.sData(0)
        io.shapeSData1 <> shape.io.sData(1)
        io.shapeMData <> shape.io.mData


        shape.io.introut := io.shapeMDataCmd.introut

    }


    register.io.regSData <> regSData
    //    (0 until 5).foreach(i => {
    //        register.ins(i) <> conv.io.instruction(i)
    //    })
    //    (0 until 6).foreach(i => {
    //        register.ins(i) <> shape.io.instruction(i)
    //    })
    (0 until ConvNum).foreach(i => {
        register.io.convInstruction(i) <> conv(i).io.instruction
        register.convInst(i).convState <> conv(i).io.state
        register.convInst(i).convControl <> conv(i).io.control
    })
    register.io.shapeInstruction <> shape.io.instruction
    register.shapeState <> shape.io.state
    register.shapeControl <> shape.io.control


}

object Npu extends App {
    //    SpinalVerilog(new Npu(ConvConfig(8, 8, 8, 12, 8192, 512, 416, 2048, 1), ShapeConfig(8, 8, 416, 10, 1024)))
    //    TotalTcl(Config.filePath + File.separator + "tcl", Config.filePath).genTotalTcl
    val clockCfg = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = HIGH)
    SpinalConfig(headerWithDate = true, oneFilePerComponent = false, defaultConfigForClockDomains = clockCfg, targetDirectory = Config.filePath + File.separator + "rtl").generateVerilog(new Npu(ConvConfig(8, 16, 16, 12, 4096, 512, 10, 10, 2048), ShapeConfig(8, 16, 640, 12, 2048)))
    TotalTcl(Config.filePath + File.separator + "tcl", Config.filePath).genTotalTcl
}

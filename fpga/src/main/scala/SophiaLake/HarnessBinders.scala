package chipyard.fpga.sophialake

import chisel3._

import freechips.rocketchip.jtag.{JTAGIO}
import freechips.rocketchip.subsystem.{PeripheryBusKey}
import freechips.rocketchip.tilelink.{TLBundle}
import freechips.rocketchip.diplomacy.{LazyRawModuleImp}
import org.chipsalliance.diplomacy.nodes.{HeterogeneousBag}
import sifive.blocks.devices.uart.{UARTPortIO, UARTParams}
import sifive.blocks.devices.jtag.{JTAGPins, JTAGPinsFromPort}
import sifive.blocks.devices.pinctrl.{BasePin}
import sifive.fpgashells.shell._
import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.shell.xilinx._
import sifive.fpgashells.clocks._
import chipyard._
import chipyard.harness._
import chipyard.iobinders._
import testchipip.serdes._
import testchipip.serdes.old._

class WithSophiaLakeUARTTSI extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: UARTTSIPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val harnessIO = IO(new UARTPortIO(port.io.uartParams)).suggestName("uart_tsi")
    harnessIO <> port.io.uart
    val packagePinsWithPackageIOs = Seq(
      ("T21" , IOPin(harnessIO.rxd)),
      ("V22", IOPin(harnessIO.txd)))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      ath.xdc.addPackagePin(io, pin)
      ath.xdc.addIOStandard(io, "LVCMOS33")
      ath.xdc.addIOB(io)
    } }
  }
})

class WithSophiaLakeTDDRTL extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: TLMemPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val bundles = ath.ddrClient.out.map(_._1)
    val ddrClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
    bundles.zip(ddrClientBundle).foreach { case (bundle, io) => bundle <> io }
    ddrClientBundle <> port.io
  }
})

class WithSophiaLakeSerialTLToGPIO extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: OldSerialTLPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val harnessIO = IO(chiselTypeOf(port.io)).suggestName(s"serial_tl_old_${port.portId}")
    harnessIO <> port.io
    harnessIO match {
      case io: testchipip.serdes.old.DecoupledSerialIO => {
        val clkIO = io match {
          case io: testchipip.serdes.old.InternalSyncSerialIO => IOPin(io.clock_out)
          case io: testchipip.serdes.old.ExternalSyncSerialIO => IOPin(io.clock_in)
        }

        val packagePinsWithPackageIOs = if (port.portId == 0) {
          Seq(
            ("A15", clkIO),
            ("G13", IOPin(io.in.valid)),
            ("L14", IOPin(io.in.ready)),
            ("A13", IOPin(io.in.bits, 0)),
            ("H13", IOPin(io.in.bits, 1)),
            ("J14", IOPin(io.in.bits, 2)),
            ("K16", IOPin(io.in.bits, 3)),
            ("L16", IOPin(io.in.bits, 4)),
            ("N19", IOPin(io.in.bits, 5)),
            ("G18", IOPin(io.in.bits, 6)),
            ("G17", IOPin(io.in.bits, 7)),

            ("D14", IOPin(io.out.valid)),
            ("G16", IOPin(io.out.ready)),
            ("K13", IOPin(io.out.bits, 0)),
            ("M13", IOPin(io.out.bits, 1)),
            ("M15", IOPin(io.out.bits, 2)),
            ("A14", IOPin(io.out.bits, 3)),
            ("M16", IOPin(io.out.bits, 4)),
            ("A16", IOPin(io.out.bits, 5)),
            ("F16", IOPin(io.out.bits, 6)),
            ("F13", IOPin(io.out.bits, 7)),
          )
        } else {
          Seq(

            ("B15", clkIO),
            ("L15", IOPin(io.out.valid)),
            ("F14", IOPin(io.out.ready)),
            ("C13", IOPin(io.out.bits, 0)),
            ("E13", IOPin(io.in.valid)),
            ("L13", IOPin(io.in.ready)),
            ("B13", IOPin(io.in.bits, 0)),
          )
        }
        packagePinsWithPackageIOs foreach { case (pin, io) => {
          ath.xdc.addPackagePin(io, pin)
          ath.xdc.addIOStandard(io, "LVCMOS12")
        }}

        // // Don't add IOB to the clock, if its an input
        io match {
          case io: testchipip.serdes.old.InternalSyncSerialIO => packagePinsWithPackageIOs foreach { case (pin, io) => {
            ath.xdc.addIOB(io)
          }}
          case io: testchipip.serdes.old.ExternalSyncSerialIO => packagePinsWithPackageIOs.drop(1).foreach { case (pin, io) => {
            ath.xdc.addIOB(io)
          }}
        }

        ath.sdc.addClock("ser_tl_clock", clkIO, 100)
        ath.sdc.addGroup(pins = Seq(clkIO))
        ath.xdc.clockDedicatedRouteFalse(clkIO)
      }
    }
  }
})




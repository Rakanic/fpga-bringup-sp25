package chipyard.fpga.sophialake

import chisel3._

import freechips.rocketchip.jtag.{JTAGIO}
import freechips.rocketchip.subsystem.{PeripheryBusKey}
import freechips.rocketchip.tilelink.{TLBundle}
import freechips.rocketchip.diplomacy.{LazyRawModuleImp}
import org.chipsalliance.diplomacy.nodes.{HeterogeneousBag}
import sifive.blocks.devices.uart.{UARTPortIO, UARTParams}
import sifive.blocks.devices.spi.{SPIPortIO, SPIParams}
import sifive.blocks.devices.pwm.{PWMPortIO, PWMParams}
// import sifive.blocks.devices.i2c.{I2CPort, I2CParams}

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
import testchipip.spi.{SPISubordinateIO}


//==========================================================
// DSP24 Sophia Lake Tilelink GPIO Config
//==========================================================
class WithDSP24SophiaLakeSerialTLToGPIO extends HarnessBinder({
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
            ("L15", clkIO),

            ("A16", IOPin(io.out.valid)),
            ("M15", IOPin(io.out.ready)),
            ("J14", IOPin(io.out.bits, 0)),
            ("G16", IOPin(io.out.bits, 1)),
            ("K16", IOPin(io.out.bits, 2)),
            ("A15", IOPin(io.out.bits, 3)),
            ("L14", IOPin(io.out.bits, 4)),
            ("E13", IOPin(io.out.bits, 5)),
            ("F14", IOPin(io.out.bits, 6)),
            ("G13", IOPin(io.out.bits, 7)),

            ("B13", IOPin(io.in.valid)),
            ("A14", IOPin(io.in.ready)),
            ("M16", IOPin(io.in.bits, 0)),
            ("N19", IOPin(io.in.bits, 1)),
            ("L16", IOPin(io.in.bits, 2)),
            ("H13", IOPin(io.in.bits, 3)),
            ("A13", IOPin(io.in.bits, 4)),
            ("D14", IOPin(io.in.bits, 5)),
            ("G17", IOPin(io.in.bits, 6)),
            ("K13", IOPin(io.in.bits, 7)),
          )
        } else {
          Seq(
            ("F13", clkIO),
            ("C13", IOPin(io.in.valid)),
            ("M13", IOPin(io.in.ready)),
            ("F16", IOPin(io.in.bits, 0)),
            ("G18", IOPin(io.out.valid)),
            ("B15", IOPin(io.out.ready)),
            ("L13", IOPin(io.out.bits, 0)),
          )
        }
        packagePinsWithPackageIOs foreach { case (pin, io) => {
          ath.xdc.addPackagePin(io, pin)
          ath.xdc.addIOStandard(io, "LVCMOS12")
        }}

        // Don't add IOB to the clock, if its an input
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


//==========================================================
// BearlyML 25 Sophia Lake Tilelink GPIO Config
//==========================================================
class WithBML25SophiaLakeSerialTLToGPIO extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: SerialTLPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val harnessIO = IO(chiselTypeOf(port.io)).suggestName("serial_tl")
    harnessIO <> port.io
    harnessIO match {
      case io: DecoupledPhitIO => {
        val clkIO = io match {
          case io: HasClockOut => IOPin(io.clock_out)
          case io: HasClockIn => IOPin(io.clock_in)
        }
        val packagePinsWithPackageIOs = {
          Seq(
            ("D17", clkIO),

            ("C15", IOPin(io.out.valid)),
            ("D16", IOPin(io.out.ready)),
            ("C17", IOPin(io.out.bits.phit, 0)),
            ("A18", IOPin(io.out.bits.phit, 1)),
            ("B21", IOPin(io.out.bits.phit, 2)),
            ("A20", IOPin(io.out.bits.phit, 3)),
            ("B18", IOPin(io.out.bits.phit, 4)),
            ("A19", IOPin(io.out.bits.phit, 5)),
            ("C18", IOPin(io.out.bits.phit, 6)),
            ("B17", IOPin(io.out.bits.phit, 7)),

            ("A21", IOPin(io.in.valid)),
            ("B20", IOPin(io.in.ready)),
            ("H19", IOPin(io.in.bits.phit, 0)),
            ("G20", IOPin(io.in.bits.phit, 1)),
            ("C14", IOPin(io.in.bits.phit, 2)),
            ("H20", IOPin(io.in.bits.phit, 3)),
            ("F19", IOPin(io.in.bits.phit, 4)),
            ("E22", IOPin(io.in.bits.phit, 5)),
            ("C19", IOPin(io.in.bits.phit, 6)),
            ("C22", IOPin(io.in.bits.phit, 7)),
          )
        }
        
        packagePinsWithPackageIOs foreach { case (pin, io) => {
          ath.xdc.addPackagePin(io, pin)
          ath.xdc.addIOStandard(io, "LVCMOS12")
        }}

        
        // Don't add IOB to the clock, if its an input
        io match {
          case io: DecoupledInternalSyncPhitIO => packagePinsWithPackageIOs foreach { case (pin, io) => {
            ath.xdc.addIOB(io)
          }}
          case io: DecoupledExternalSyncPhitIO => packagePinsWithPackageIOs.drop(1).foreach { case (pin, io) => {
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


//==========================================================
// DSP25 Sophia Lake Tilelink GPIO Config
//==========================================================
class WithDSP25SophiaLakeSerialTLToGPIO extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: SerialTLPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val harnessIO = IO(chiselTypeOf(port.io)).suggestName("serial_tl")
    harnessIO <> port.io
    harnessIO match {
      case io: DecoupledPhitIO => {
        val clkIO = io match {
          case io: HasClockOut => IOPin(io.clock_out)
          case io: HasClockIn => IOPin(io.clock_in)
        }
        val packagePinsWithPackageIOs = {
          Seq(
            ("A21", clkIO),

            ("B20", IOPin(io.out.valid)),
            ("E16", IOPin(io.out.ready)),
            ("C22", IOPin(io.out.bits.phit, 0)),
            ("E19", IOPin(io.out.bits.phit, 1)),
            ("M15", IOPin(io.out.bits.phit, 2)),
            ("A15", IOPin(io.out.bits.phit, 3)),
            ("C13", IOPin(io.out.bits.phit, 4)),
            ("L15", IOPin(io.out.bits.phit, 5)),
            ("G20", IOPin(io.out.bits.phit, 6)),
            ("F13", IOPin(io.out.bits.phit, 7)),

            ("G13", IOPin(io.in.valid)),
            ("E17", IOPin(io.in.ready)),
            ("G15", IOPin(io.in.bits.phit, 0)),
            ("H14", IOPin(io.in.bits.phit, 1)),
            ("J20", IOPin(io.in.bits.phit, 2)),
            ("J17", IOPin(io.in.bits.phit, 3)),
            ("N20", IOPin(io.in.bits.phit, 4)),
            ("N19", IOPin(io.in.bits.phit, 5)),
            ("J19", IOPin(io.in.bits.phit, 6)),
            ("A14", IOPin(io.in.bits.phit, 7)),
          )
        }
        
        packagePinsWithPackageIOs foreach { case (pin, io) => {
          ath.xdc.addPackagePin(io, pin)
          ath.xdc.addIOStandard(io, "LVCMOS12")
        }}

        
        // Don't add IOB to the clock, if its an input
        io match {
          case io: DecoupledInternalSyncPhitIO => packagePinsWithPackageIOs foreach { case (pin, io) => {
            ath.xdc.addIOB(io)
          }}
          case io: DecoupledExternalSyncPhitIO => packagePinsWithPackageIOs.drop(1).foreach { case (pin, io) => {
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











//==========================================================
// Generic Harness Binder Classes
//==========================================================
class WithSophiaLakeUARTTSI extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: UARTTSIPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val harnessIO = IO(new UARTPortIO(port.io.uartParams)).suggestName("uart_tsi")
    harnessIO <> port.io.uart
    val packagePinsWithPackageIOs = Seq(
      ("T21" , IOPin(harnessIO.rxd)),
      ("U21", IOPin(harnessIO.txd)))

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


class WithJohnPMODPWM extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: PWMPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val harnessIO = IO(new PWMPortIO(new PWMParams(address=0x0))).suggestName("john_pwm")
    harnessIO <> port.io
    val packagePinsWithPackageIOs = Seq(
      ("P15" , IOPin(harnessIO.gpio(0))),
      ("N17" , IOPin(harnessIO.gpio(1))),
      ("R16" , IOPin(harnessIO.gpio(2))),
      ("N14" , IOPin(harnessIO.gpio(3))))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      ath.xdc.addPackagePin(io, pin)
      ath.xdc.addIOStandard(io, "LVCMOS33")
      ath.xdc.addIOB(io)
    } }
  }
})


class WithJohnPMODI2C extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: I2CPinsPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val harnessIO = IO(new sifive.blocks.devices.i2c.I2CPinsIO).suggestName("john_i2c")
    harnessIO <> port.io
    val packagePinsWithPackageIOs = Seq(
      ("P17" , IOPin(harnessIO.scl)),
      ("P16", IOPin(harnessIO.sda)))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      ath.xdc.addPackagePin(io, pin)
      ath.xdc.addIOStandard(io, "LVCMOS33")
      ath.xdc.addIOB(io)
    } }
  }
})


class WithRegulatorI2C extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: I2CPinsPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val harnessIO = IO(new sifive.blocks.devices.i2c.I2CPinsIO).suggestName("john_i2c")
    harnessIO <> port.io
    val packagePinsWithPackageIOs = Seq(
      ("AB18" , IOPin(harnessIO.scl)),
      ("AA18", IOPin(harnessIO.sda)))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      ath.xdc.addPackagePin(io, pin)
      ath.xdc.addIOStandard(io, "LVCMOS33")
      ath.xdc.addIOB(io)
      ath.xdc.addPullup(io)
    } }
  }
})


// class WithJohnPMODSPI extends HarnessBinder({
//   case (th: HasHarnessInstantiators, port: SPIPort, chipId: Int) => {
//     val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
//     val harnessIO = IO(new SPIPortIO(new SPIParams(rAddress=0, csWidth=2))).suggestName("john_spi")
//     harnessIO <> port.io
//     val packagePinsWithPackageIOs = Seq(
//       ("AB16" , IOPin(harnessIO.sck)),
//       ("AA15" , IOPin(harnessIO.cs(0))),
//       ("AB17" , IOPin(harnessIO.cs(1))),
//       ("AA13" , IOPin(harnessIO.dq(0))), //MOSI
//       ("AB13" , IOPin(harnessIO.dq(1))))  //MISO

//     packagePinsWithPackageIOs foreach { case (pin, io) => {
//       ath.xdc.addPackagePin(io, pin)
//       ath.xdc.addIOStandard(io, "LVCMOS33")
//       ath.xdc.addIOB(io)
//     } }
//   }
// })


class WithSophiaLakeFTDISPITSI extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: SPITSIPort, chipId: Int) => {
    val ath = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[SophiaLakeHarness]
    val harnessIO = IO(new SPISubordinateIO()).suggestName("spitsi")
    harnessIO <> port.io.spi

    ath.sdc.addClock("SPITTSI_SCK", IOPin(harnessIO.sck), 30)
    ath.sdc.addGroup(clocks = Seq("SPITTSI_SCK"))
    ath.xdc.clockDedicatedRouteFalse(IOPin(harnessIO.sck))
    val packagePinsWithPackageIOs = Seq(
      ("T21", IOPin(harnessIO.sck)),
      ("V22", IOPin(harnessIO.mosi)),
      ("R21", IOPin(harnessIO.miso)),
      ("U21", IOPin(harnessIO.csn))
    )
    
    packagePinsWithPackageIOs foreach { case (pin, io) => {
      ath.xdc.addPackagePin(io, pin)
      ath.xdc.addIOStandard(io, "LVCMOS33")
      ath.xdc.addPullup(io)
    } }

    // ath.other_leds(8) := port.io.dropped
    // ath.other_leds(9) := port.io.tsi2tl_state(0)
    // ath.other_leds(10) := port.io.tsi2tl_state(1)
    // ath.other_leds(11) := port.io.tsi2tl_state(2)
    // ath.other_leds(12) := port.io.tsi2tl_state(3)

  }
})
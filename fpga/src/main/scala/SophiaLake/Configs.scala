// See LICENSE for license details.
package chipyard.fpga.sophialake

import org.chipsalliance.cde.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import org.chipsalliance.diplomacy._
import org.chipsalliance.diplomacy.lazymodule._
import freechips.rocketchip.system._
import freechips.rocketchip.tile._

import sifive.blocks.devices.uart._
import sifive.fpgashells.shell.{DesignKey}

import testchipip.serdes._

import chipyard.{BuildSystem}
import org.chipsalliance.cde.config.{Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.{MBUS, SBUS}
import testchipip.soc.{OBUS}

// don't use FPGAShell's DesignKey
class WithNoDesignKey extends Config((site, here, up) => {
  case DesignKey => (p: Parameters) => new SimpleLazyRawModule()(p)
})

// By default, this uses the on-board USB-UART for the TSI-over-UART link
class WithSophiaLakeTweaks(freqMHz: Double = 10) extends Config(
  // new WithSophiaLakeFTDISPITSI ++
  // new testchipip.tsi.WithSPITSIClient ++

  new WithSophiaLakeUARTTSI ++
  new WithSophiaLakeTDDRTL ++

  new WithNoDesignKey ++
  new chipyard.harness.WithSerialTLTiedOff ++

  new testchipip.tsi.WithUARTTSIClient(initBaudRate = 921600) ++
  
  new chipyard.harness.WithHarnessBinderClockFreqMHz(freqMHz) ++
  new chipyard.config.WithUniformBusFrequencies(freqMHz) ++
  new chipyard.harness.WithAllClocksFromHarnessClockInstantiator ++
  new chipyard.clocking.WithPassthroughClockGenerator ++

  new chipyard.config.WithTLBackingMemory ++
  new freechips.rocketchip.subsystem.WithExtMemSize(BigInt(256) << 22) ++
  new testchipip.serdes.WithNoSerialTL ++

  new freechips.rocketchip.subsystem.WithoutTLMonitors)

class SophiaLakeDSP24Config extends Config(
  new testchipip.soc.WithMbusScratchpad(base = 0x10080000L, size = 256 * 1024) ++
  new chipyard.config.WithBroadcastManager ++
  
  new SophiaLakeBringupHostConfig2)


// A simple config demonstrating a "bringup prototype" to bringup the ChipLikeRocketconfig
class SophiaLakeBringupHostConfig2 extends Config(
  new WithSophiaLakeSerialTLToGPIO ++
  new WithJohnPMODPWM ++
  new chipyard.iobinders.WithI2CIOCells ++
  new chipyard.iobinders.WithPWMPunchthrough ++
  new WithJohnPMODI2C ++
  // new WithJohnPMODSPI ++
  new WithSophiaLakeTweaks(freqMHz = 50) ++
  new chipyard.iobinders.WithOldSerialTLPunchthrough ++                // Don't generate IOCells for the serial TL (this design maps to FPGA)

  //=============================
  // Setup a bunch of peripherals to provide to the chip over SerialTL-P
  //=============================
  // new chipyard.config.WithSPI(address = 0x18000000, csWidth=2) ++
  new chipyard.config.WithI2C(address = 0x10081000) ++
  new chipyard.config.WithPWM(address = 0x10080000, channels = 4) ++

  //=============================
  // Setup the SerialTL side on the bringup device
  //=============================
  new testchipip.serdes.old.WithSerialTL(Seq(
    testchipip.serdes.old.SerialTLParams(
      manager = Some(testchipip.serdes.old.SerialTLManagerParams(
        memParams = Seq(
          testchipip.serdes.old.ManagerRAMParams(                            // Bringup platform can access all memory from 0 to DRAM_BASE
            address = BigInt("00000000", 16),
            size    = BigInt("10070000", 16)),
          testchipip.serdes.old.ManagerRAMParams(                            // Bringup platform can access all memory from 0 to DRAM_BASE
            address = BigInt("14000000", 16),
            size    = BigInt("6C000000", 16)),
      ))),
      client = Some(testchipip.serdes.old.SerialTLClientParams()),                                        // Allow chip to access this device's memory (DRAM)
      phyParams = testchipip.serdes.old.InternalSyncSerialParams(width=8, freqMHz = 50)), // bringup platform provides the clock
    
    testchipip.serdes.old.SerialTLParams(
      manager = None,
      client = Some(testchipip.serdes.old.SerialTLClientParams()),                                        // Allow chip to access this device's memory (DRAM)
      phyParams = testchipip.serdes.old.InternalSyncSerialParams(width=1, freqMHz = 50)), // bringup platform provides the clock   
    )) ++

  //============================
  // Setup bus topology on the bringup system
  //============================
  new testchipip.soc.WithOffchipBusClient(SBUS,                                 // offchip bus hangs off the SBUS
    blockRange = AddressSet.misaligned(0x80000000L, (BigInt(1) << 30) * 4)) ++ // offchip bus should not see the main memory of the testchip, since that can be accessed directly
  new testchipip.soc.WithOffchipBus ++                                          // offchip bus
  new chipyard.NoCoresConfig)

class RegProgConfig extends Config(
  new chipyard.iobinders.WithI2CIOCells ++
  new WithRegulatorI2C ++
  new WithSophiaLakeTweaks(freqMHz = 50) ++

  new chipyard.config.WithI2C(address = 0x10081000) ++
  new chipyard.NoCoresConfig)
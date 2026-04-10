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

import chisel3._
import chipyard.harness.{HarnessClockInstantiator, HarnessClockInstantiatorKey}
import chipyard.clocking.{ClockDividerN}


//==========================================================
// DSP 25 Sophia Lake Config
//==========================================================
class SophiaLakeDSP25Config extends Config(
  new WithDSP25SophiaLakeSerialTLToGPIO ++
  new testchipip.serdes.WithSerialTL(Seq(
    testchipip.serdes.SerialTLParams(
      manager = Some(testchipip.serdes.SerialTLManagerParams(
        memParams = Seq(testchipip.serdes.ManagerRAMParams(                            // Bringup platform can access all memory from 0 to DRAM_BASE
          address = BigInt("00000000", 16),
          size    = BigInt("80000000", 16)
        )),
        slaveWhere = OBUS
      )),
      client = Some(testchipip.serdes.SerialTLClientParams()),                                        // Allow chip to access this device's memory (DRAM)
      phyParams = testchipip.serdes.DecoupledInternalSyncSerialPhyParams(phitWidth=8, flitWidth=16, freqMHz = 50) // bringup platform provides the clock
    )
  )) ++
  new SophiaLakeConfig(freqMHz = 50))


  class SophiaLakeDSP25C2CConfig extends Config(
    new WithDSP25C2CSophiaLakeSerialTLToGPIO ++
    new testchipip.serdes.WithSerialTL(Seq(
      testchipip.serdes.SerialTLParams(
        manager = Some(testchipip.serdes.SerialTLManagerParams(
          memParams = Seq(testchipip.serdes.ManagerRAMParams(
            address = BigInt("00000000", 16),
            size    = BigInt("80000000", 16)         // extended to match port 1's 32-bit address width
          )),
          slaveWhere = OBUS
        )),
        client = Some(testchipip.serdes.SerialTLClientParams()),
        phyParams = testchipip.serdes.DecoupledInternalSyncSerialPhyParams(phitWidth=8, flitWidth=16, freqMHz = 50)
      ),
      testchipip.serdes.SerialTLParams(
        manager = Some(testchipip.serdes.SerialTLManagerParams(
          memParams = Seq(testchipip.serdes.ManagerRAMParams(
            address = BigInt("c0000000", 16),
            size    = BigInt("40000000", 16)       // 1 GiB window
          )),
          slaveWhere = MBUS
        )),
        client = None,
        phyParams = testchipip.serdes.CreditedSourceSyncSerialPhyParams(phitWidth=1, flitWidth=16, freqMHz = 5)
      )
    )) ++
    new WithDividerHarnessClockInstantiator ++                  // Override AllClocksFromHarnessClockInstantiator to support clock division
    new SophiaLakeConfig(freqMHz = 50))



//==========================================================
// BearlyML 25 Chip-to-Chip Sophia Lake Config
//==========================================================
class SophiaLakeBML25C2CConfig extends Config(
  new WithBML25C2CSophiaLakeSerialTLToGPIO ++
  new testchipip.serdes.WithSerialTL(Seq(
    testchipip.serdes.SerialTLParams(
      manager = Some(testchipip.serdes.SerialTLManagerParams(
        memParams = Seq(testchipip.serdes.ManagerRAMParams(
          address = BigInt("00000000", 16),
          size    = BigInt("80000000", 16)         // 0-3 GiB, extended to match port 1's 32-bit address width
        )),
        slaveWhere = OBUS
      )),
      client = Some(testchipip.serdes.SerialTLClientParams()),
      phyParams = testchipip.serdes.DecoupledInternalSyncSerialPhyParams(phitWidth=8, flitWidth=16, freqMHz = 50)
    ),
    testchipip.serdes.SerialTLParams(
      manager = None,
      client = Some(testchipip.serdes.SerialTLClientParams()),                                                                        // BML only initiates requests to DSP25, no incoming
      phyParams = testchipip.serdes.CreditedSourceSyncSerialPhyParams(phitWidth=1, flitWidth=16, freqMHz = 5)
    )
  )) ++
  new testchipip.soc.WithScratchpad(base = BigInt("c0000000", 16), size = 16 << 10, busWhere = MBUS) ++
  new WithDividerHarnessClockInstantiator ++                    // Override AllClocksFromHarnessClockInstantiator to support clock division
  new SophiaLakeConfig(freqMHz = 50))


//==========================================================
// BearlyML 25 Sophia Lake Config
//==========================================================
class SophiaLakeBML25Config extends Config(
  new WithBML25SophiaLakeSerialTLToGPIO ++
  new testchipip.serdes.WithSerialTL(Seq(
    testchipip.serdes.SerialTLParams(
      manager = Some(testchipip.serdes.SerialTLManagerParams(
        memParams = Seq(testchipip.serdes.ManagerRAMParams(                            // Bringup platform can access all memory from 0 to DRAM_BASE
          address = BigInt("00000000", 16),
          size    = BigInt("80000000", 16) )) )),
      client = Some(testchipip.serdes.SerialTLClientParams()),                                        // Allow chip to access this device's memory (DRAM)
      phyParams = testchipip.serdes.DecoupledInternalSyncSerialPhyParams(phitWidth=8, flitWidth=16, freqMHz = 50) // bringup platform provides the clock
    )
  )) ++
  new SophiaLakeConfig(freqMHz = 50))








//==========================================================
// DSP24 Sophia Lake Config
//==========================================================
class SophiaLakeDSP24Config extends Config(
  new WithDSP24SophiaLakeSerialTLToGPIO ++
  new chipyard.iobinders.WithSerialTLPunchthrough ++                // Don't generate IOCells for the serial TL (this design maps to FPGA)
  new chipyard.iobinders.WithOldSerialTLPunchthrough ++             // Don't generate IOCells for the serial TL (this design maps to FPGA)
  new testchipip.serdes.WithNoSerialTL ++
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
      client = Some(testchipip.serdes.old.SerialTLClientParams()),                        // Allow chip to access this device's memory (DRAM)
      phyParams = testchipip.serdes.old.InternalSyncSerialParams(width=8, freqMHz = 50)), // bringup platform provides the clock
    
    testchipip.serdes.old.SerialTLParams(
      manager = None,
      client = Some(testchipip.serdes.old.SerialTLClientParams()),                        // Allow chip to access this device's memory (DRAM)
      phyParams = testchipip.serdes.old.InternalSyncSerialParams(width=1, freqMHz = 50)), // bringup platform provides the clock   
    )) ++
  new SophiaLakeConfig(freqMHz = 50))


























//==========================================================
// Harness clock instantiator that supports clock division
//==========================================================
class DividerHarnessClockInstantiator extends HarnessClockInstantiator {
  def instantiateHarnessClocks(refClock: Clock, refClockFreqMHz: Double): Unit = {
    val refFreqHz = refClockFreqMHz * 1000 * 1000
    for ((name, (freqHz, clock)) <- clockMap) {
      if (freqHz == refFreqHz) {
        clock := refClock
      } else {
        val divBy = math.round(refFreqHz / freqHz).toInt
        require(divBy > 1 && math.abs(refFreqHz / divBy - freqHz) < 1.0,
          s"Reference clock ${refClockFreqMHz} MHz cannot be evenly divided to get ${freqHz / 1e6} MHz for clock $name")
        val divider = Module(new ClockDividerN(divBy))
        divider.io.clk_in := refClock
        clock := divider.io.clk_out
      }
    }
  }
}

class WithDividerHarnessClockInstantiator extends Config((site, here, up) => {
  case HarnessClockInstantiatorKey => () => new DividerHarnessClockInstantiator
})


//==========================================================
// Generic Config Classes
//==========================================================

// don't use FPGAShell's DesignKey
class WithNoDesignKey extends Config((site, here, up) => {
  case DesignKey => (p: Parameters) => new SimpleLazyRawModule()(p)
})


// By default, this uses the on-board USB-UART for the TSI-over-UART link
class WithSophiaLakeTweaks(freqMHz: Double) extends Config(
  new WithNoDesignKey ++
  new WithSophiaLakeUARTTSI ++
  new WithSophiaLakeTDDRTL ++
  new chipyard.config.WithBroadcastManager ++
  new chipyard.harness.WithSerialTLTiedOff ++

  new testchipip.tsi.WithUARTTSIClient(initBaudRate = 921600) ++
  new chipyard.harness.WithHarnessBinderClockFreqMHz(freqMHz) ++
  new chipyard.config.WithUniformBusFrequencies(freqMHz) ++
  new chipyard.harness.WithAllClocksFromHarnessClockInstantiator ++
  new chipyard.clocking.WithPassthroughClockGenerator ++
  new chipyard.config.WithTLBackingMemory ++
  new freechips.rocketchip.subsystem.WithExtMemSize(BigInt(256) << 22) ++
  new freechips.rocketchip.subsystem.WithoutTLMonitors)



// A simple config demonstrating a "bringup prototype" to bringup the ChipLikeRocketconfig
class SophiaLakeConfig(freqMHz: Double) extends Config(
  //============================
  // Setup bus topology on the bringup system
  //============================
  new testchipip.soc.WithOffchipBusClient(SBUS,                                 // offchip bus hangs off the SBUS
    blockRange = AddressSet.misaligned(0x80000000L, (BigInt(1) << 30) * 4)) ++ // offchip bus should not see the main memory of the testchip, since that can be accessed directly
  new testchipip.soc.WithOffchipBus ++                                          // offchip bus

  new WithSophiaLakeTweaks(freqMHz = freqMHz) ++
  new chipyard.NoCoresConfig)


class RegProgConfig extends Config(
  new chipyard.iobinders.WithI2CIOCells ++
  new WithRegulatorI2C ++
  new WithSophiaLakeTweaks(freqMHz = 50) ++

  new chipyard.config.WithI2C(address = 0x10081000) ++
  new chipyard.NoCoresConfig)
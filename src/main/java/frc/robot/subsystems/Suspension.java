package frc.robot.subsystems;

import frc.robot.Constants;
import frc.robot.loops.Loop;
import frc.robot.loops.Looper;
import frc.lib.util.drivers.Talon.CANTalonFactory;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.LimitSwitchNormal;
import com.ctre.phoenix.motorcontrol.LimitSwitchSource;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

/**
 * The suspension subsystem consists of dynamo motors that are meant to send one ball at a time into the shooter.
 * There are ir sensors placed before and after the suspension to sense blockage or emptiness of the hopper.
 * The main things this subsystem has to are feed fuel and unjam
 * 
 *
 */
/*
public class Suspension extends Subsystem {
    
   

    private static Suspension sInstance = null;

    public static Suspension getInstance() {
        if (sInstance == null) {
            sInstance = new Suspension();
        }
        return sInstance;
    }

    private TalonSRX mRaiseTalon, mWheelTalon;
   
    public Suspension() {
        
        //Talon Initialization 
        mRaiseTalon = CANTalonFactory.createTalon(Constants.kSuspensionBackLiftTalonID, 
        false, NeutralMode.Brake, FeedbackDevice.QuadEncoder, 0, false);

        mRaiseTalon = CANTalonFactory.setupHardLimits(mRaiseTalon, LimitSwitchSource.Deactivated,
        LimitSwitchNormal.Disabled, false, LimitSwitchSource.FeedbackConnector, LimitSwitchNormal.NormallyOpen,true);
        
        mRaiseTalon = CANTalonFactory.setupSoftLimits(mRaiseTalon, true, (int) Math.round(Constants.kSuspensionLiftSoftLimit*Constants.kSuspensionLiftTicksPerInch),
        false, 0);
        
        mRaiseTalon = CANTalonFactory.tuneLoops(mRaiseTalon, 0, Constants.kSuspensionBackLiftTalonP,
        Constants.kSuspensionBackLiftTalonI, Constants.kSuspensionBackLiftTalonD, Constants.kSuspensionBackLiftTalonF);

        //Wheel Talon
        mWheelTalon = CANTalonFactory.createTalon(Constants.kSuspensionWheelTalonID, 
        true, NeutralMode.Brake, FeedbackDevice.QuadEncoder, 0, false);

        mWheelTalon = CANTalonFactory.setupHardLimits(mRaiseTalon, LimitSwitchSource.Deactivated,
        LimitSwitchNormal.Disabled, false, LimitSwitchSource.Deactivated, LimitSwitchNormal.Disabled,false);
        
        mWheelTalon = CANTalonFactory.setupSoftLimits(mRaiseTalon, false, 0,
        false, 0);

       
    }

    private void setLimitClear(boolean e){
           mRaiseTalon.configClearPositionOnLimitR(e,0);    
    }

    public enum ControlState {
        IDLE,
        HOMING,
    }

    private ControlState mControlState = ControlState.IDLE;
    private ControlState mWantedState = ControlState.IDLE;

    private double mCurrentStateStartTime;
    private boolean mStateChanged;

    private Loop mLoop = new Loop() {
        @Override
        public void onStart(double timestamp) {
            stop();
            synchronized (Suspension.this) {
                mControlState = ControlState.IDLE;
                mStateChanged = true;
                mCurrentStateStartTime = timestamp;
                mWheelTalon.setSelectedSensorPosition(0);
            }
        }

        @Override
        public void onLoop(double timestamp) {
            synchronized (Suspension.this) {
                ControlState newState;
                switch (mControlState) {
                case IDLE:
                    newState = handleIdle();
                    break;
                case HOMING:
                    newState = handleHoming();
                    break;   
                default:
                    newState = ControlState.IDLE;
                }
                if (newState != mControlState) {
                    System.out.println("Suspension state " + mControlState + " to " + newState);
                    mControlState = newState;
                    mCurrentStateStartTime = timestamp;
                    mStateChanged = true;
                } else {
                    mStateChanged = false;
                }
                positionUpdater();
                positionWheelUpdater();
            }
        }

        @Override
        public void onStop(double timestamp) {
            stop();
        }
    };


    private ControlState defaultIdleTest(){
        if(mControlState == mWantedState){
            mWantedState=ControlState.IDLE;
            return ControlState.IDLE; 
        }
        else return mWantedState;
    }

    private ControlState handleIdle() {
        if(mStateChanged){
            stopMotor();
        }
        
       return defaultIdleTest();
    }

    private boolean hasHomed = false;

    private ControlState handleHoming(){
        if(mStateChanged){
            hasHomed=false;
            setLimitClear(true);
            mRaiseTalon.set(ControlMode.PercentOutput,-.1);
            mRaiseTalon.setSelectedSensorPosition(-1);
        }

        if(!hasHomed&&mRaiseTalon.getSelectedSensorPosition()==0){
            hasHomed=true;
            mTravelingPosition=.1;
            setPosition(0);
        }

        ControlState newState;

        if(hasHomed){
            if(atPosition()){
            newState= defaultIdleTest();
            }else{
                newState= mWantedState;
            }
        }else{
            newState= ControlState.HOMING;
        }

        if(newState!=ControlState.HOMING)setLimitClear(false);
        return newState;
    }

   
    

    //CLOSED LOOP CONTROL
    private double mWantedPosition = .1;
    private double mTravelingPosition = 0;
    
    public synchronized void setPosition(double pos){
        if(pos>=Constants.kSuspensionLiftSoftLimit)pos=Constants.kSuspensionLiftSoftLimit;
        else if(pos<0)pos=0;
        mWantedPosition=pos;
        
       // System.out.println("Set wanted pos to "+pos);
    }
private boolean jog=false;
    public void jog(double amount){
        setPosition(mWantedPosition+=amount);
        jog=true;
    }


   public boolean atPosition(){
      if(Math.abs(mWantedPosition-getPosition())<=Constants.kSuspensionLiftTolerance){
          return true;
      }else{
          return false;
      }
       
   }

   public double getPosition(){
       return mRaiseTalon.getSelectedSensorPosition()/Constants.kSuspensionLiftTicksPerInch;
   }

   private void positionUpdater(){
           
    if(hasHomed&&mWantedPosition!=mTravelingPosition){

        mTravelingPosition=mWantedPosition;
        if(!jog)System.out.println("Suspension to "+mTravelingPosition);
        jog=false;
        mRaiseTalon.set(ControlMode.Position, mTravelingPosition*Constants.kSuspensionLiftTicksPerInch);
    }
}



//WHEEL ___________________________________________________________

private double mWantedWheelPosition = .1;
    private double mTravelingWheelPosition = 0;
    
    public synchronized void setWheelPosition(double pos){
       
        mWantedWheelPosition=pos;
        
       // System.out.println("Set wanted pos to "+pos);
    }

    public void jogWheel(double amount){
        setWheelPosition(mWantedWheelPosition+=amount);
    }


   public boolean atWheelPosition(){
      if(Math.abs(mWantedWheelPosition-getPosition())<=Constants.kSuspensionWheelTolerance){
          return true;
      }else{
          return false;
      }
       
   }

   public double getWheelPosition(){
       return mWheelTalon.getSelectedSensorPosition()/Constants.kSuspensionWheelTicksPerInch;
   }

   private void positionWheelUpdater(){
           
    if(mWantedPosition!=mTravelingWheelPosition){

        mTravelingWheelPosition=mWantedWheelPosition;
        mWheelTalon.set(ControlMode.Position, mTravelingWheelPosition*Constants.kSuspensionWheelTicksPerInch);
    }
}
  
   private synchronized void stopMotor(){
        mRaiseTalon.set(ControlMode.Disabled,0);
        mWheelTalon.set(ControlMode.Disabled, 0);
    }

    public synchronized void setWantedState(ControlState state) {
        mWantedState = state;
    }

    @Override
    public void outputToSmartDashboard() {
        // SmartDashboard.putNumber("suspension_speed", mMasterTalon.get() / Constants.kSuspensionSensorGearReduction);
    }

    @Override
    public void stop() {
        setWantedState(ControlState.IDLE);
    }

    @Override
    public void zeroSensors() {
    }


    @Override
    public void registerEnabledLoops(Looper in) {
        in.register(mLoop);
    }

    public boolean checkSystem() {
        System.out.println("Testing Suspension.-----------------------------------");
        boolean failure=false;       
        return !failure;
    }

}*/
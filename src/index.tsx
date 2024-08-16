import { NativeEventEmitter, NativeModules } from 'react-native';

type PedometerType = {
  isSupported(): Promise<boolean>;
  startStepCounter(start: string): any;
  stopStepCounter(): any;
};

const { Pedometer } = NativeModules;

export const PedometerEventListener = new NativeEventEmitter(Pedometer);

export default Pedometer as PedometerType;

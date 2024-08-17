import { NativeEventEmitter, NativeModules } from 'react-native';

type PedometerType = {
  isSupported(): Promise<boolean>;
  isRunning(): Promise<boolean>;
  syncStepCounter(dates: string[]): Promise<Record<string, number>>;
  startStepCounter(): void;
  stopStepCounter(): void;
};

const { Pedometer } = NativeModules;

export const PedometerEventListener = new NativeEventEmitter(Pedometer);

export default Pedometer as PedometerType;

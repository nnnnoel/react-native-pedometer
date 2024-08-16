import CoreMotion

@objc(Pedometer)
class Pedometer: RCTEventEmitter {

    private let defaults = UserDefaults.standard
    private let pedometer: CMPedometer = CMPedometer()
    var numberOfSteps: Int! = 0

    override func supportedEvents() -> [String]! {
        return ["StepCounter"]
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true
    }

    @objc(isSupported:withRejecter:)
    func isSupported(resolve: RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        if CMPedometer.isStepCountingAvailable() {
            resolve(true)
        } else {
            resolve(false)
        }
    }

    @objc(syncStepCounter:resolver:withRejecter:)
    func syncStepCounter(_ arr:NSArray, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) -> Void {

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        dateFormatter.timeZone = TimeZone(identifier: "KST")

        var i:Int = 0;
        var dict = Dictionary<String, NSNumber>()
        for target in arr {
            let key:String = "\(target)";

            let date1 = dateFormatter.date(from: key + " 00:00:00")!
            let date2 = dateFormatter.date(from: key + " 23:59:59")!

            self.pedometer.queryPedometerData( from: date1, to: date2 ) { data, error in
                if let steps = data?.numberOfSteps {
                    dict[key] = steps
                }

                i += 1;
                if (arr.count <= i) {
                    resolve(dict);
                }
            }
        }
    }

    @objc(startStepCounter)
    func startStepCounter() {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        dateFormatter.timeZone = TimeZone(identifier: "KST")

        var stepStart = defaults.string( forKey: "step_start" ) ?? dateFormatter.string(from: Date())
        defaults.set(stepStart, forKey: "step_start")

        let date = Calendar.current.date(bySettingHour: 0, minute: 0, second: 0, of: Date())!

        self.pedometer.startUpdates(from: date) { (data, error) in
            guard let pedometerData = data, error == nil else {
                print("There was an error getting the data: \(String(describing: error))")
                return
            }

            let pedDataSteps = pedometerData.numberOfSteps.intValue
            DispatchQueue.main.async {
                if self.numberOfSteps != pedDataSteps {
                    self.numberOfSteps = pedDataSteps
                    self.sendEvent(withName: "StepCounter", body: ["steps": self.numberOfSteps])
                }
            }
        }
    }

    @objc(stopStepCounter)
    func stopStepCounter() -> Void {
        defaults.removeObject(forKey: "step_start")
        pedometer.stopUpdates()
        if #available(iOS 10.0, *) {
            pedometer.stopEventUpdates()
        } else {
            // Fallback on earlier versions
        }
    }

    func checkAuthStatus() -> Bool {
        var pedometerAuth = false
        if #available(iOS 11.0, *) {
            switch CMPedometer.authorizationStatus() {
                case CMAuthorizationStatus.authorized:
                    pedometerAuth = true
                default:
                    break
            }
        } else {
            // Fallback on earlier versions
        }
        return pedometerAuth
    }

}

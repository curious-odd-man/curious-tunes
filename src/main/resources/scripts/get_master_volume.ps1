Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public class AudioUtils {
    [DllImport("ole32.dll")]
    static extern int CoCreateInstance(
        ref Guid clsid, IntPtr inner, uint context,
        ref Guid uuid, out IntPtr ptr);

    [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"),
     InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMMDeviceEnumerator {
        int EnumAudioEndpoints(int dataFlow, int stateMask, out IntPtr devices);
        int GetDefaultAudioEndpoint(int dataFlow, int role, out IntPtr device);
    }

    [ComImport, Guid("D666063F-1587-4E43-81F1-B948E807363F"),
     InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMMDevice {
        int Activate(ref Guid iid, uint clsCtx, IntPtr activationParams, out IntPtr iface);
        int OpenPropertyStore(uint access, out IntPtr store);
        int GetId(out string id);
        int GetState(out uint state);
    }

    [ComImport, Guid("5CDF2C82-841E-4546-9722-0CF74078229A"),
     InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioEndpointVolume {
        int RegisterControlChangeNotify(IntPtr notify);
        int UnregisterControlChangeNotify(IntPtr notify);
        int GetChannelCount(out uint count);
        int SetMasterVolumeLevel(float level, ref Guid ctx);
        int SetMasterVolumeLevelScalar(float level, ref Guid ctx);
        int GetMasterVolumeLevel(out float level);
        int GetMasterVolumeLevelScalar(out float level);
    }

    public static double GetMasterVolume() {
        Guid clsid    = new Guid("BCDE0395-E52F-467C-8E3D-C4579291692E");
        Guid enumIid  = new Guid("A95664D2-9614-4F35-A746-DE8DB63617E6");
        Guid devIid   = new Guid("D666063F-1587-4E43-81F1-B948E807363F");
        Guid volIid   = new Guid("5CDF2C82-841E-4546-9722-0CF74078229A");

        IntPtr enumPtr;
        CoCreateInstance(ref clsid, IntPtr.Zero, 0x17, ref enumIid, out enumPtr);
        var enumerator = (IMMDeviceEnumerator)Marshal.GetObjectForIUnknown(enumPtr);

        IntPtr devicePtr;
        enumerator.GetDefaultAudioEndpoint(0, 1, out devicePtr);
        var device = (IMMDevice)Marshal.GetObjectForIUnknown(devicePtr);

        IntPtr volPtr;
        device.Activate(ref volIid, 0x17, IntPtr.Zero, out volPtr);
        var vol = (IAudioEndpointVolume)Marshal.GetObjectForIUnknown(volPtr);

        float level;
        vol.GetMasterVolumeLevelScalar(out level);
        return Math.Round(level, 4);
    }
}
"@

[AudioUtils]::GetMasterVolume()
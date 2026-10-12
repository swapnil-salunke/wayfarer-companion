// Renders the Wayfarer app icon (1024x1024, opaque) with CoreGraphics.
// Usage: swiftc -O render_icon.swift -o /tmp/render_icon && /tmp/render_icon out.png [androidDrawableDir]
// The same coordinates are used by the Android vector drawables in androidApp/src/main/res/drawable/.
import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

let S: CGFloat = 1024
func rgb(_ hex: UInt32, _ a: CGFloat = 1) -> CGColor {
    CGColor(red: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: a)
}
let ink = rgb(0x12262B), ridgeBack = rgb(0x2E5B63), ridgeFront = rgb(0x1F6F78), amber = rgb(0xE8A33D), ground = rgb(0xF2F5F3)

let ctx = CGContext(data: nil, width: 1024, height: 1024, bitsPerComponent: 8, bytesPerRow: 0,
                    space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
// Work in a top-left origin, like the SVG/Android coordinate systems.
ctx.translateBy(x: 0, y: S); ctx.scaleBy(x: 1, y: -1)

func poly(_ pts: [(CGFloat, CGFloat)], _ color: CGColor) {
    ctx.setFillColor(color); ctx.beginPath(); ctx.move(to: CGPoint(x: pts[0].0, y: pts[0].1))
    pts.dropFirst().forEach { ctx.addLine(to: CGPoint(x: $0.0, y: $0.1)) }
    ctx.closePath(); ctx.fillPath()
}
func circle(_ cx: CGFloat, _ cy: CGFloat, _ r: CGFloat, _ color: CGColor) {
    ctx.setFillColor(color); ctx.fillEllipse(in: CGRect(x: cx - r, y: cy - r, width: 2 * r, height: 2 * r))
}

// Background + halo behind the sun
ctx.setFillColor(ink); ctx.fill(CGRect(x: 0, y: 0, width: S, height: S))
circle(690, 330, 96, amber)

// Ridges (bleed off the edges so any launcher mask stays filled)
poly([(0, 760), (300, 440), (500, 660), (690, 500), (1024, 800), (1024, 1024), (0, 1024)], ridgeBack)
poly([(0, 880), (250, 650), (430, 820), (620, 700), (1024, 980), (1024, 1024), (0, 1024)], ridgeFront)

// Trail: dots at equal arc-length spacing along a cubic Bezier, shrinking with distance (perspective).
let p0 = CGPoint(x: 330, y: 1030), p1 = CGPoint(x: 760, y: 940), p2 = CGPoint(x: 230, y: 820), p3 = CGPoint(x: 500, y: 650)
func bez(_ t: CGFloat) -> CGPoint {
    let u = 1 - t
    return CGPoint(x: u*u*u*p0.x + 3*u*u*t*p1.x + 3*u*t*t*p2.x + t*t*t*p3.x,
                   y: u*u*u*p0.y + 3*u*u*t*p1.y + 3*u*t*t*p2.y + t*t*t*p3.y)
}
var samples: [(CGFloat, CGPoint)] = [], acc: CGFloat = 0, prev = bez(0)
for i in 0...400 {
    let t = CGFloat(i) / 400, p = bez(t)
    acc += hypot(p.x - prev.x, p.y - prev.y); prev = p; samples.append((acc, p))
}
let total = acc, dotCount = 8
var trail: [(CGFloat, CGFloat, CGFloat)] = []
for i in 0..<dotCount {
    let f = CGFloat(i) / CGFloat(dotCount - 1)
    let target = total * (0.03 + 0.94 * f)
    let p = samples.first(where: { $0.0 >= target })!.1
    trail.append((p.x, p.y, 28 - 18 * f))
}
trail.forEach { circle($0.0, $0.1, $0.2, ground) }

// Android vector layers (viewport 108, art scaled from the 1024 grid). Optional 2nd/3rd args: output dir.
if CommandLine.arguments.count > 2 {
    let dir = CommandLine.arguments[2], k = 108.0 / 1024.0
    func f(_ v: CGFloat) -> String { String(format: "%.2f", Double(v) * k) }
    func path(_ pts: [(CGFloat, CGFloat)], _ hex: String) -> String {
        let d = "M" + pts.map { "\(f($0.0)),\(f($0.1))" }.joined(separator: "L") + "Z"
        return "    <path android:fillColor=\"\(hex)\" android:pathData=\"\(d)\"/>\n"
    }
    func dot(_ x: CGFloat, _ y: CGFloat, _ r: CGFloat, _ hex: String) -> String {
        let d = "M\(f(x - r)),\(f(y))a\(f(r)),\(f(r)) 0 1,0 \(f(2 * r)),0a\(f(r)),\(f(r)) 0 1,0 -\(f(2 * r)),0Z"
        return "    <path android:fillColor=\"\(hex)\" android:pathData=\"\(d)\"/>\n"
    }
    let head = "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"108\" android:viewportHeight=\"108\">\n"
    var bg = head + path([(0,0),(1024,0),(1024,1024),(0,1024)], "#12262B")
    bg += path([(0, 760), (300, 440), (500, 660), (690, 500), (1024, 800), (1024, 1024), (0, 1024)], "#2E5B63")
    bg += path([(0, 880), (250, 650), (430, 820), (620, 700), (1024, 980), (1024, 1024), (0, 1024)], "#1F6F78") + "</vector>\n"
    var fg = head + dot(690, 330, 96, "#E8A33D")
    trail.forEach { fg += dot($0.0, $0.1, $0.2, "#F2F5F3") }
    fg += "</vector>\n"
    try! bg.write(toFile: dir + "/ic_launcher_background.xml", atomically: true, encoding: .utf8)
    try! fg.write(toFile: dir + "/ic_launcher_foreground.xml", atomically: true, encoding: .utf8)
    print("wrote android vectors to \(dir)")
}

let img = ctx.makeImage()!
let url = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "icon.png")
let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(dest, img, nil); CGImageDestinationFinalize(dest)
print("wrote \(url.path)")

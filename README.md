# PNG Palette and Priority Editor for SGDK

PPP Editor for SGDK is a Java-based tool for editing palette and priority masks on indexed 8bpp PNG images, specifically designed for SGDK (Sega Genesis Development Kit) projects.

With PPP Editor, you can:
✅ Load and visualize indexed 8bpp PNG images.
✅ Edit palette indices (0-3) and priority flags (0/1) on an 8×8 tile grid.
✅ Select multiple tiles with lasso selection or CTRL + click.
✅ Export the final image with the applied palette and priority encoding.
✅ Use a batch mode for automated processing via the command line.

Features
🎨 Image & Mask Handling
Load indexed 8bpp PNG images.
Automatically generate a default mask upon image loading.
Save and load .msk mask files to preserve tile properties.

🖱️ Editing Capabilities
Left Click & Drag → Lasso selection of multiple tiles.
CTRL + Left Click → Add/remove multiple selection areas.
Right Click → Open a property editor to modify:

Palette index (0-3)
Priority (0 = low, 1 = high)

Keyboard Shortcuts:
H → Set priority High (1)
L → Set priority Low (0)
0, 1, 2, 3 → Change palette index of selected tiles

🔍 Visualization & Navigation
Grid Overlay:

Light gray grid for normal tiles.
Blue borders for tiles with HIGH priority.

Palette indices displayed with color-coded text (white, green, pink, blue).

Zoom Options:
SHIFT + Mouse Wheel → Smooth zooming
Arrow Keys → Scroll image

🏭 Batch Mode (Command Line Processing)
Run the editor via the command line for automated processing:

java -jar PPPE4SGDK.jar --b <image_path> <mask_path> <export_path>
Example:

java -jar PPPE4SGDK.jar --b bgb.png mask.msk bgb_palprio.png



Installation & Usage
1️⃣ Download & Run

Download PPPE4SGDK.jar

Run with Java:
java -jar PPPE4SGDK.jar

2️⃣ Load an Image
Go to Image → Load Image and select an indexed 8bpp PNG.

3️⃣ Edit the Mask
Use the selection tools and property editor or keyboards shortcuts.

4️⃣ Save / Export
Save the mask: Mask → Save Mask (.msk).
Export the final image: Image → Export Image (.png).


Screenshots
(Add screenshots of the interface, selection tools, and exported images here.)

Contributing
Feel free to report issues or suggest new features! 🚀

License
This project is open-source under the MIT License.



![Separator](https://imgur.com/gZDWWY6.png)

# Fusion Connected Textures

Fusion is a library for both Forge and Fabric which adds additional texture and model types such as connected textures to be used in resource packs and which allows other mods to add additional texture and model types!

## 📖 Wiki
### For resource packs:
https://github.com/SuperMartijn642/Fusion/wiki#for-resource-packs
### For mod developers:
https://github.com/SuperMartijn642/Fusion/wiki#for-mod-developers

![Separator](https://imgur.com/gZDWWY6.png)

### CurseForge and Modrinth
For more info and downloads, check out the project on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fusion-connected-textures) or [Modrinth](https://modrinth.com/mod/fusion-connected-textures)!

![Separator](https://imgur.com/gZDWWY6.png)

## 🖼️ Texture types

### Connected texture
Connected texture are textures with a special layout which allows them to connect to other blocks when combined with the connecting model type. Fusion offers two connected texture layouts, full and simple.
Here is an example of the simple layout:

<img width='500' src='https://imgur.com/avP2A0U.png' alt='Simple connected texture example'>

### Scrolling texture
Scrolling textures are a type of animated texture which scrolls over an image. The scrolling can occur from any corner to any other corner of the texture, even diagonal!
Here is an example of a conveyor belt:

<img width='300' src='https://imgur.com/CUJoRk7.png' alt='Scrolling texture conveyor example'>

 

![Separator](https://imgur.com/gZDWWY6.png)

## 🧊 Model types

### Connected model
Conected models are models which will connect to other blocks when used in conjunction with the connected texture type. Connections can be specified for specific blocks and states.
Here is an example of an oak tiles block which connects to itself and acacia tiles:

<table>
<tr><td>

```json
{
   ...
   "type": "connecting",
   "connections": [
      {
         "type": "is_same_block"
      },
      {
         "type": "match_block",
         "block": "acacia_tiles"
      }
   ]
   ...
}
```

</td><td>
<img width='500' src='https://imgur.com/AoSdjrP.gif' alt='Connected model example'>
</td></tr>
</table>

![Separator](https://imgur.com/gZDWWY6.png)

## FAQ
### Can I use Fusion in my modpack?
Yes, you are allowed use Fusion in your modpack
### Do I need to install Fusion on the server?
No, Fusion is only required on the client and does nothing when installed on a server
### Why does Fusion not work with Sodium?
Sodium ignores the Fabric rendering api. To solve this install [Indium](https://modrinth.com/mod/indium).

![Separator](https://imgur.com/gZDWWY6.png)

### Discord
For future content, upcoming mods, and discussion, feel free to join the SuperMartijn642 discord server!  
[<img width='400' src='https://imgur.com/IG1us6p.png'>](https://discord.gg/QEbGyUYB2e)

![Separator](https://imgur.com/gZDWWY6.png)

### Legal Stuff
Fusion is the property of SuperMartijn642 and is protected under copyright law and may not be altered or reuploaded without direct permission from SuperMartijn642.

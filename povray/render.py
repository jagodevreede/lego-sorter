import bpy
import os, random
import re

def reset():
    pattern = re.compile("^\\d+$")
    for obj in bpy.data.objects:
        if pattern.match(obj.name):
            obj.location[2] = -20

def randomizeObj(obj):
    bpy.data.materials["Material.001"].node_tree.nodes["Principled BSDF"].inputs[0].default_value = (random.uniform(0, 1), random.uniform(0, 1), random.uniform(0, 1), 1)
    obj.location[0] = random.uniform(0, 1)
    obj.location[1] = random.uniform(0, 1)
    #obj.rotation_euler[0] = random.uniform(0, 6.28493)
    #obj.rotation_euler[1] = random.uniform(0, 6.28493)
    obj.rotation_euler[2] = random.uniform(0, 6.28493)


def dropObj(b):
    obj = bpy.data.objects[b]
    mx = obj.matrix_world
    minz = min((mx @ v.co)[2] for v in obj.data.vertices)
    mx.translation.z -= minz

def snapZ(obj, prefix, brick):
    for x in range(1, 80):
        randomizeObj(obj)
        # dropObj(brick)
        camera = bpy.data.objects['Camera']
        camera.rotation_euler[0] = random.uniform(1.15, 1.22)
        camera.rotation_euler[1] = random.uniform(-0.087, 0.087)
        camera.rotation_euler[2] = random.uniform(0.69, 0.87)
        camera.location[0] = random.uniform(-8, 8)
        camera.location[1] = random.uniform(-8, 8)
        camera.location[2] = random.uniform(6.5, 9)
        light = bpy.data.objects['Light']
        light.location[0] = random.uniform(-4, 2.5)
        light.location[1] = random.uniform(-3, -1)
        light.location[2] = random.uniform(4, 8)
        light.data.energy = random.uniform(800, 1200)
        light.data.color = (random.uniform(0.8, 1), random.uniform(0.8, 1), random.uniform(0.8, 1))
        light.data.shadow_soft_size = random.uniform(1, 3)
        backgroundImg = random.choice(os.listdir("/Users/jagodevreede/git/lego-sorter/povray/backgrounds/"))
        bpy.data.images[
            'unnamed2.jpeg'].filepath = '/Users/jagodevreede/git/lego-sorter/povray/backgrounds/' + backgroundImg
        bpy.context.scene.render.filepath = '/Users/jagodevreede/git/lego-sorter/povray/' + brick + '/' + prefix + str(x) + '.png'
        bpy.ops.render.render(write_still=True)

def generateObj(brick):
    reset()
    bpy.context.scene.render.resolution_x = 500
    bpy.context.scene.render.resolution_y = 500
    # Deselect all
    for obj in bpy.data.objects:
        obj.select_set(False)
    obj = bpy.data.objects[brick]
    obj.location[2] = 1.6
    bpy.context.object.rotation_euler[1] = 3.14159
    bpy.context.object.rotation_euler[0] = 0
    snapZ(obj, 'y180x0_', brick)



generateObj('3002')
generateObj('3004')
generateObj('3020')
generateObj('3022')
generateObj('3623')
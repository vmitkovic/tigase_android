#!/usr/bin/python
import os
import subprocess
from xml.etree import ElementTree
fileName = os.getcwd() + "/res/values/strings.xml"
print fileName
buildNumber = subprocess.check_output('git describe --tags'.split()).strip('\n')
tree = ElementTree.parse(fileName)
strings = tree.findall("string")
app_strings = [s for s in strings if s.get("name") == "app_version"]
for s in app_strings:
    s.text = buildNumber
tree.write(fileName)

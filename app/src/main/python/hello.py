from fontTools.ttx import main
import sys

def extract(ttf):
    sys.argv = ['ttx','-z','extfile',ttf]
    main()

def build(ttx):
    sys.argv = ['ttx',ttx]
    main()
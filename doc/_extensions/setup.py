"""Setup for BerryCrush Pygments lexer."""

from setuptools import setup

setup(
    name='berrycrush-lexer',
    version='1.0.0',
    description='Pygments lexer for BerryCrush scenario files',
    py_modules=['berrycrush_lexer'],
    install_requires=['Pygments'],
    entry_points={
        'pygments.lexers': [
            'berrycrush = berrycrush_lexer:BerryCrushLexer',
        ],
    },
)

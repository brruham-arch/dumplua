import sys, io, pkgutil, traceback, contextlib

def run(inp, out):
    src = pkgutil.get_data('ljdcli', 'main.txt').decode('utf-8')
    old = sys.argv
    sys.argv = ['main.py', '-f', inp, '-o', out, '-c']
    buf = io.StringIO()
    try:
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            exec(compile(src, 'main.py', 'exec'), {'__name__': '__main__', '__file__': 'main.py'})
    except SystemExit as e:
        if e.code not in (None, 0):
            return 'exit %s %s' % (e.code, buf.getvalue()[-500:])
    except Exception:
        return traceback.format_exc()[-800:]
    finally:
        sys.argv = old
    return ''
